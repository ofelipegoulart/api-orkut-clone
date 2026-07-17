package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.*;
import com.orkutclone.api.model.*;
import com.orkutclone.api.model.enums.TestimonialStatus;
import com.orkutclone.api.repository.*;
import com.orkutclone.api.repository.projection.CommunityOverviewProjection;
import com.orkutclone.api.repository.projection.FriendOverviewProjection;
import com.orkutclone.api.repository.projection.RatingSummaryProjection;
import com.orkutclone.api.repository.projection.TestimonialOverviewProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
public class ProfileOverviewService {

    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final UserProfileGeneralRepository generalRepository;
    private final UserProfileSocialRepository socialRepository;
    private final UserProfileProfessionalRepository professionalRepository;
    private final UserProfilePersonalRepository personalRepository;
    private final ProfileFriendRepository profileFriendRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final ProfileTestimonialRepository profileTestimonialRepository;
    private final ProfileRatingRepository profileRatingRepository;
    private final ProfileStatisticsService profileStatisticsService;

    @Transactional
    @Cacheable(cacheNames = "profileOverview", keyGenerator = "profileOverviewKeyGenerator")
    public ProfileOverviewDTO getOverview(UUID userId) {
        User viewer = authenticatedUser();
        UUID targetUserId = userId != null ? userId : viewer.getId();
        boolean selfView = targetUserId.equals(viewer.getId());

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UserProfile coreProfile = selfView
                ? getOrCreateCoreProfile(viewer)
                : profileRepository.findByUserId(targetUserId).orElse(null);

        UserProfileGeneral general = coreProfile == null ? null : loadGeneral(coreProfile, selfView);
        UserProfileSocial social = coreProfile == null ? null : loadSocial(coreProfile, selfView);
        UserProfileProfessional professional = coreProfile == null ? null : loadProfessional(coreProfile, selfView);
        UserProfilePersonal personal = coreProfile == null ? null : loadPersonal(coreProfile, selfView);

        ProfileStatistics statistics = profileStatisticsService.getOrCreateSnapshot(targetUserId);
        RatingSummaryProjection ratingsSummary = profileRatingRepository.findSummaryByTargetId(targetUserId);
        ProfileRating viewerRating = profileRatingRepository.findByTargetIdAndRaterId(targetUserId, viewer.getId()).orElse(null);

        List<ProfileOverviewDTO.FriendCardDTO> friends = toFriendCards(
            profileFriendRepository.findOverviewByUserId(targetUserId, PageRequest.of(0, 9)),
            viewer.getId());

        List<ProfileOverviewDTO.TestimonialDTO> testimonialsSent = toTestimonialCards(
            profileTestimonialRepository.findByAuthorIdOrderByCreatedAtDesc(targetUserId),
            false);

        List<ProfileOverviewDTO.TestimonialDTO> testimonialsReceived = toTestimonialCards(
            selfView
                ? profileTestimonialRepository.findByTargetIdOrderByCreatedAtDesc(targetUserId)
                : profileTestimonialRepository.findByTargetIdAndStatusOrderByCreatedAtDesc(targetUserId, TestimonialStatus.APPROVED),
            selfView);

        return new ProfileOverviewDTO(
                new ProfileOverviewDTO.UserSummary(target.getId(), target.getName(), target.getProfilePicture(), target.getStatusMessage()),
                general == null ? null : toGeneralDTO(general),
                social == null ? null : toSocialDTO(social),
                professional == null ? null : toProfessionalDTO(professional),
                personal == null ? null : toPersonalDTO(personal),
            friends,
            toCommunityCards(communityMembershipRepository.findOverviewByUserId(targetUserId, PageRequest.of(0, 9))),
            testimonialsSent,
            testimonialsReceived,
            new ProfileOverviewDTO.ShortcutsDTO(
                statistics.getScrapsCount(),
                statistics.getFriendsCount(),
                statistics.getCommunitiesCount(),
                statistics.getTestimonialsCount(),
                statistics.getPhotosCount(),
                statistics.getFansCount()),
            new ProfileOverviewDTO.RatingsDTO(
                chooseRating(viewerRating == null ? null : viewerRating.getLegalPercentage(), ratingsSummary.getLegalPercentage()),
                chooseRating(viewerRating == null ? null : viewerRating.getTrustworthyPercentage(), ratingsSummary.getTrustworthyPercentage()),
                chooseRating(viewerRating == null ? null : viewerRating.getSexyPercentage(), ratingsSummary.getSexyPercentage()))
        );
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private UserProfile getOrCreateCoreProfile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile profile = new UserProfile();
                    profile.setUser(user);
                    return profileRepository.save(profile);
                });
    }

    private UserProfileGeneral loadGeneral(UserProfile profile, boolean createIfMissing) {
        return generalRepository.findByProfileId(profile.getId())
                .orElseGet(() -> {
                    if (!createIfMissing) {
                        return null;
                    }
                    UserProfileGeneral general = new UserProfileGeneral();
                    general.setProfile(profile);
                    return generalRepository.save(general);
                });
    }

    private UserProfileSocial loadSocial(UserProfile profile, boolean createIfMissing) {
        return socialRepository.findByProfileId(profile.getId())
                .orElseGet(() -> {
                    if (!createIfMissing) {
                        return null;
                    }
                    UserProfileSocial social = new UserProfileSocial();
                    social.setProfile(profile);
                    return socialRepository.save(social);
                });
    }

    private UserProfileProfessional loadProfessional(UserProfile profile, boolean createIfMissing) {
        return professionalRepository.findByProfileId(profile.getId())
                .orElseGet(() -> {
                    if (!createIfMissing) {
                        return null;
                    }
                    UserProfileProfessional professional = new UserProfileProfessional();
                    professional.setProfile(profile);
                    return professionalRepository.save(professional);
                });
    }

    private UserProfilePersonal loadPersonal(UserProfile profile, boolean createIfMissing) {
        return personalRepository.findByProfileId(profile.getId())
                .orElseGet(() -> {
                    if (!createIfMissing) {
                        return null;
                    }
                    UserProfilePersonal personal = new UserProfilePersonal();
                    personal.setProfile(profile);
                    return personalRepository.save(personal);
                });
    }

    private GeneralProfileDTO toGeneralDTO(UserProfileGeneral g) {
        return new GeneralProfileDTO(
                g.getFirstName(), g.getLastName(), g.getGender(), g.getRelationshipStatus(),
                g.getBirthMonth(), g.getBirthDay(), g.getBirthDatePrivacy(),
                g.getBirthYear(), g.getBirthYearPrivacy(),
                g.getCity(), g.getState(), g.getZipCode(), g.getCountry(),
                safeList(g.getLanguages()), g.getLanguagesPrivacy(),
                g.getHighSchool(), g.getHighSchoolPrivacy(),
                g.getCollege(), g.getCollegePrivacy(),
                g.getCompany(), g.getCompanyPrivacy(),
                safeList(g.getInterestedIn()), g.getDatingPreference()
        );
    }

    private SocialProfileDTO toSocialDTO(UserProfileSocial s) {
        return new SocialProfileDTO(
                s.getChildren(), s.getEthnicity(), s.getReligion(), s.getPoliticalView(),
                s.getSexualOrientation(), s.getSexualOrientationPrivacy(),
                safeList(s.getHumor()), safeList(s.getStyle()),
                s.getSmoking(), s.getDrinking(), s.getPets(), s.getLivingWith(),
                s.getHometown(), s.getWebsite(), s.getAboutMe(), s.getPassions(),
                s.getSports(), s.getActivities(), s.getBooks(), s.getMusic(),
                s.getTvShows(), s.getMovies(), s.getCuisines()
        );
    }

    private ProfessionalProfileDTO toProfessionalDTO(UserProfileProfessional p) {
        return new ProfessionalProfileDTO(
                p.getEducation(), p.getSchool(), p.getCollege(), p.getCourse(),
                p.getDegree(), p.getGraduationYear(), p.getProfession(), p.getSector(),
                p.getCompany(), p.getJobDescription(), p.getWorkPhone(),
                p.getProfessionalSkills(), p.getProfessionalInterests()
        );
    }

    private PersonalProfileDTO toPersonalDTO(UserProfilePersonal p) {
        return new PersonalProfileDTO(
                p.getEyeColor(), p.getHairColor(), p.getHeight(), p.getBodyType(),
                p.getAppearance(), p.getBodyArt(), p.getPerfectMatch(),
                safeList(p.getAttractions()), p.getCantStand(), p.getIdealFirstDate(),
                p.getPastRelationshipsLessons(), p.getWhatStandsOut(), p.getFavoriteBodyPart(),
                p.getFiveEssentials(), p.getInMyRoom()
        );
    }

    private List<ProfileOverviewDTO.FriendCardDTO> toFriendCards(List<FriendOverviewProjection> projections, UUID viewerId) {
        return projections.stream()
                .map(projection -> new ProfileOverviewDTO.FriendCardDTO(
                        projection.getId(),
                        projection.getName(),
                        projection.getAvatar(),
                        projection.getFriendsCount() == null ? 0L : projection.getFriendsCount(),
                        viewerId == null ? 0L : profileFriendRepository.countMutualFriends(viewerId, projection.getId())))
                .toList();
    }

    private List<ProfileOverviewDTO.CommunityCardDTO> toCommunityCards(List<CommunityOverviewProjection> projections) {
        return projections.stream()
                .map(projection -> new ProfileOverviewDTO.CommunityCardDTO(
                        projection.getId(),
                        projection.getName(),
                        projection.getIcon(),
                        projection.getMemberCount() == null ? 0L : projection.getMemberCount()))
                .toList();
    }

        private List<ProfileOverviewDTO.TestimonialDTO> toTestimonialCards(List<? extends ProfileTestimonial> testimonials, boolean includeStatus) {
        return testimonials.stream()
                .map(testimonial -> new ProfileOverviewDTO.TestimonialDTO(
                        testimonial.getId(),
                        testimonial.getAuthor().getId(),
                        testimonial.getAuthor().getName(),
                        testimonial.getAuthor().getProfilePicture(),
                        testimonial.getMessage(),
                testimonial.getStatus().name(),
                        testimonial.getCreatedAt() == null ? null : testimonial.getCreatedAt().atOffset(ZoneOffset.UTC)))
                .toList();
    }

    private double chooseRating(Double viewerRating, Double averageRating) {
        return viewerRating != null ? viewerRating : (averageRating == null ? 0D : averageRating);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}