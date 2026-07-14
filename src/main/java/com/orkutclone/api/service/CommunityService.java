package com.orkutclone.api.service;

import com.orkutclone.api.dto.community.CategoryOptionDTO;
import com.orkutclone.api.dto.community.CommunityDetailDTO;
import com.orkutclone.api.dto.community.CommunityDashboardDTO;
import com.orkutclone.api.dto.community.CommunityIconResponse;
import com.orkutclone.api.dto.community.CommunityListItemDTO;
import com.orkutclone.api.dto.community.CreateCommunityRequest;
import com.orkutclone.api.dto.community.MyCommunityDTO;
import com.orkutclone.api.model.Community;
import com.orkutclone.api.model.CommunityFeatureSettings;
import com.orkutclone.api.model.CommunityLocation;
import com.orkutclone.api.model.CommunityMembership;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.CommunityCategory;
import com.orkutclone.api.model.enums.MembershipStatus;
import com.orkutclone.api.repository.CommunityMembershipRepository;
import com.orkutclone.api.repository.CommunityRepository;
import com.orkutclone.api.repository.projection.CommunityListItemProjection;
import com.orkutclone.api.repository.projection.MyCommunityProjection;
import com.orkutclone.api.support.AvatarStorageService;
import com.orkutclone.api.support.LastPostFormatter;
import com.orkutclone.api.support.UploadedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final String DEFAULT_LANGUAGE = "Português";
    private static final int MAX_PAGE_SIZE = 100;

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final ProfileStatisticsService profileStatisticsService;
    private final AvatarStorageService imageStorage;

    /** Feeds the category dropdown so the labels live in one place only. */
    public List<CategoryOptionDTO> listCategories() {
        return Arrays.stream(CommunityCategory.values())
                .map(category -> new CategoryOptionDTO(category.name(), category.getLabel()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityListItemDTO> listByCategory(CommunityCategory category, int page, int size) {
        Instant now = Instant.now();
        return communityRepository.findByCategory(category, pageRequest(page, size)).stream()
                .map(projection -> toListItem(projection, now))
                .toList();
    }

    /** Blank input matches everything, so it short-circuits to an empty list instead. */
    @Transactional(readOnly = true)
    public List<CommunityListItemDTO> searchByName(String name, int page, int size) {
        if (name == null || name.isBlank()) {
            return List.of();
        }
        Instant now = Instant.now();
        return communityRepository.searchByName(name.trim(), pageRequest(page, size)).stream()
                .map(projection -> toListItem(projection, now))
                .toList();
    }

    /** Communities the current user owns, belongs to, or is awaiting approval in. */
    @Transactional(readOnly = true)
    public List<MyCommunityDTO> listMine() {
        Instant now = Instant.now();
        return communityRepository.findByUserInvolvement(authenticatedUser().getId()).stream()
                .map(projection -> new MyCommunityDTO(
                        projection.getId(),
                        projection.getName(),
                        projection.getIcon(),
                        projection.getMembersCount(),
                        toOffsetDateTime(projection.getLastPostDate()),
                        LastPostFormatter.format(projection.getLastPostDate(), now),
                        toRelation(projection)))
                .toList();
    }

    /** Ownership outranks membership: an owner is always shown as OWNER, never as MEMBER. */
    private String toRelation(MyCommunityProjection projection) {
        if (projection.getOwnedByUser()) {
            return "OWNER";
        }
        return projection.getMembershipStatus() == MembershipStatus.PENDING ? "PENDING" : "MEMBER";
    }

    private CommunityListItemDTO toListItem(CommunityListItemProjection projection, Instant now) {
        return new CommunityListItemDTO(
                projection.getId(),
                projection.getName(),
                projection.getIcon(),
                projection.getMembersCount(),
                toOffsetDateTime(projection.getLastPostDate()),
                LastPostFormatter.format(projection.getLastPostDate(), now));
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public CommunityDetailDTO create(CreateCommunityRequest request) {
        User current = authenticatedUser();

        Community community = communityRepository.save(Community.builder()
                .name(request.name().trim())
                .description(request.description())
                .icon(request.icon())
                .language(blankToDefault(request.language(), DEFAULT_LANGUAGE))
                .category(request.category())
                .type(request.type())
                .contentPrivacy(request.contentPrivacy())
                .location(toLocation(request.location()))
                .features(toFeatures(request.features()))
                .owner(current)
                .membersCount(1)
                .build());

        // The owner is a member from the start, and never needs approval.
        membershipRepository.save(CommunityMembership.builder()
                .user(current)
                .community(community)
                .status(MembershipStatus.APPROVED)
                .build());

        profileStatisticsService.refreshSnapshot(current.getId());
        return toDetail(community, 1);
    }

    /**
     * Stores an image and returns its public URL, to be sent back as {@code icon} when
     * creating the community. Uploading before the community exists is what lets the form
     * submit in a single request.
     */
    public CommunityIconResponse uploadIcon(MultipartFile file) {
        UploadedImage image = UploadedImage.from(file);
        return new CommunityIconResponse(imageStorage.store(image.data(), image.extension()));
    }

    private CommunityLocation toLocation(CreateCommunityRequest.LocationRequest location) {
        if (location == null) {
            return null;
        }
        return CommunityLocation.builder()
                .city(location.city())
                .state(location.state())
                .zipCode(location.zipCode())
                .country(location.country())
                .build();
    }

    private CommunityFeatureSettings toFeatures(CreateCommunityRequest.FeatureSettingsRequest features) {
        if (features == null) {
            return CommunityFeatureSettings.builder().build();
        }
        return CommunityFeatureSettings.builder()
                .forumEnabled(orDefault(features.forumEnabled(), true))
                .forumOnHomepage(orDefault(features.forumOnHomepage(), true))
                .forumNoAnonymousPosts(orDefault(features.forumNoAnonymousPosts(), true))
                .pollsEnabled(orDefault(features.pollsEnabled(), true))
                .pollsOnHomepage(orDefault(features.pollsOnHomepage(), true))
                .eventsEnabled(orDefault(features.eventsEnabled(), true))
                .eventsOnHomepage(orDefault(features.eventsOnHomepage(), false))
                .customNewsEnabled(orDefault(features.customNewsEnabled(), false))
                .build();
    }

    private CommunityDetailDTO toDetail(Community community, long membersCount) {
        CommunityLocation location = community.getLocation();
        return new CommunityDetailDTO(
                community.getId(),
                community.getName(),
                community.getDescription(),
                community.getIcon(),
                community.getLanguage(),
                community.getCategory() == null ? null : community.getCategory().name(),
                community.getCategory() == null ? null : community.getCategory().getLabel(),
                community.getType().name(),
                community.getContentPrivacy().name(),
                location == null ? null : new CommunityDashboardDTO.LocationDTO(
                        location.getCity(), location.getState(), location.getZipCode(), location.getCountry()),
                toFeaturesDTO(community.getFeatures()),
                community.getOwner().getId(),
                membersCount,
                community.getCreatedAt() == null ? null : community.getCreatedAt().atOffset(ZoneOffset.UTC));
    }

    static CommunityDetailDTO.FeatureSettingsDTO toFeaturesDTO(CommunityFeatureSettings features) {
        return new CommunityDetailDTO.FeatureSettingsDTO(
                features.isForumEnabled(),
                features.isForumOnHomepage(),
                features.isForumNoAnonymousPosts(),
                features.isPollsEnabled(),
                features.isPollsOnHomepage(),
                features.isEventsEnabled(),
                features.isEventsOnHomepage(),
                features.isCustomNewsEnabled());
    }

    private static boolean orDefault(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
