package com.orkutclone.api.service;

import com.orkutclone.api.dto.community.CommunityDashboardDTO;
import com.orkutclone.api.model.Community;
import com.orkutclone.api.model.CommunityLocation;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.CommunityContentPrivacy;
import com.orkutclone.api.model.enums.MembershipStatus;
import com.orkutclone.api.repository.CommunityMembershipRepository;
import com.orkutclone.api.repository.CommunityRepository;
import com.orkutclone.api.repository.CommunityTopicRepository;
import com.orkutclone.api.repository.projection.CommunityMemberProjection;
import com.orkutclone.api.repository.projection.CommunityTopicBriefProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityDashboardService {

    private static final int FEATURED_MEMBERS_LIMIT = 9;
    private static final int TOPICS_LIMIT = 10;

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final CommunityTopicRepository communityTopicRepository;

    @Transactional(readOnly = true)
    public CommunityDashboardDTO getDashboard(UUID communityId) {
        Community community = communityRepository.findByIdWithOwner(communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));

        User user = authenticatedUser();
        requireContentVisibleTo(community, user);

        long membersCount = communityMembershipRepository.countByCommunityId(communityId);

        List<CommunityDashboardDTO.TopicBriefDTO> topics = communityTopicRepository
                .findBriefsByCommunityId(communityId, PageRequest.of(0, TOPICS_LIMIT))
                .stream()
                .map(this::toTopicBrief)
                .toList();

        List<CommunityDashboardDTO.MemberSummaryDTO> featuredMembers = communityMembershipRepository
                .findMembersByCommunityId(communityId, PageRequest.of(0, FEATURED_MEMBERS_LIMIT))
                .stream()
                .map(this::toMemberSummary)
                .toList();

        return new CommunityDashboardDTO(
                toCommunityInfo(community, membersCount, resolveViewerRelation(community, user)),
                topics,
                // TODO: populate once the poll subsystem exists.
                null,
                featuredMembers
        );
    }

    /** Ownership outranks membership: an owner is always shown as OWNER, never as MEMBER. */
    private String resolveViewerRelation(Community community, User user) {
        if (community.getOwner() != null && community.getOwner().getId().equals(user.getId())) {
            return "OWNER";
        }
        return communityMembershipRepository.findByUserIdAndCommunityId(user.getId(), community.getId())
                .map(membership -> membership.getStatus() == MembershipStatus.PENDING ? "PENDING" : "MEMBER")
                .orElse("NONE");
    }

    /** A community with hidden content ("Oculta") only shows its dashboard to approved members. */
    private void requireContentVisibleTo(Community community, User user) {
        if (community.getContentPrivacy() != CommunityContentPrivacy.RESTRICTED) {
            return;
        }
        boolean member = communityMembershipRepository.existsByUserIdAndCommunityIdAndStatus(
                user.getId(), community.getId(), MembershipStatus.APPROVED);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This community's content is visible to members only");
        }
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private CommunityDashboardDTO.CommunityInfo toCommunityInfo(Community community, long membersCount, String viewerRelation) {
        return new CommunityDashboardDTO.CommunityInfo(
                community.getId(),
                community.getName(),
                community.getDescription(),
                community.getIcon(),
                community.getLanguage(),
                community.getCategory() == null ? null : community.getCategory().name(),
                community.getCategory() == null ? null : community.getCategory().getLabel(),
                community.getOwner() == null ? null : community.getOwner().getId(),
                community.getOwner() == null ? null : community.getOwner().getName(),
                community.getType().name(),
                community.getContentPrivacy().name(),
                toLocation(community.getLocation()),
                CommunityService.toFeaturesDTO(community.getFeatures()),
                membersCount,
                community.getCreatedAt() == null ? null : community.getCreatedAt().atOffset(ZoneOffset.UTC),
                viewerRelation
        );
    }

    private CommunityDashboardDTO.LocationDTO toLocation(CommunityLocation location) {
        if (location == null) {
            return null;
        }
        return new CommunityDashboardDTO.LocationDTO(
                location.getCity(),
                location.getState(),
                location.getZipCode(),
                location.getCountry()
        );
    }

    private CommunityDashboardDTO.TopicBriefDTO toTopicBrief(CommunityTopicBriefProjection projection) {
        return new CommunityDashboardDTO.TopicBriefDTO(
                projection.getId(),
                projection.getTitle(),
                projection.getTotalPosts(),
                projection.getLastPostDate() == null ? null : projection.getLastPostDate().atOffset(ZoneOffset.UTC)
        );
    }

    private CommunityDashboardDTO.MemberSummaryDTO toMemberSummary(CommunityMemberProjection projection) {
        return new CommunityDashboardDTO.MemberSummaryDTO(
                projection.getId(),
                projection.getName(),
                projection.getAvatar(),
                projection.getFriendsCount() == null ? 0L : projection.getFriendsCount()
        );
    }
}
