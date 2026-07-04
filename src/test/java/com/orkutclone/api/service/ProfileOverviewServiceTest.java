package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.ProfileOverviewDTO;
import com.orkutclone.api.model.*;
import com.orkutclone.api.model.enums.Role;
import com.orkutclone.api.repository.*;
import com.orkutclone.api.model.enums.TestimonialStatus;
import com.orkutclone.api.repository.projection.RatingSummaryProjection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileOverviewServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository profileRepository;
    @Mock private UserProfileGeneralRepository generalRepository;
    @Mock private UserProfileSocialRepository socialRepository;
    @Mock private UserProfileProfessionalRepository professionalRepository;
    @Mock private UserProfilePersonalRepository personalRepository;
    @Mock private ProfileFriendRepository profileFriendRepository;
    @Mock private CommunityMembershipRepository communityMembershipRepository;
    @Mock private ProfileTestimonialRepository profileTestimonialRepository;
    @Mock private ProfileRatingRepository profileRatingRepository;
    @Mock private ProfileStatisticsService profileStatisticsService;

    @InjectMocks
    private ProfileOverviewService profileOverviewService;

    private User currentUser;
    private User otherUser;
    private UserProfile currentProfile;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Felipe Goulart")
                .email("felipe@orkut.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .name("Maria Silva")
                .email("maria@orkut.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        currentProfile = new UserProfile();
        currentProfile.setId(UUID.randomUUID());
        currentProfile.setUser(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Self view deve auto-criar perfil base e seções e preencher shortcuts")
    void shouldAutoCreateAndReturnSelfOverview() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(profileRepository.findByUserId(currentUser.getId())).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> {
            UserProfile profile = inv.getArgument(0);
            profile.setId(currentProfile.getId());
            return profile;
        });

        when(generalRepository.findByProfileId(currentProfile.getId())).thenReturn(Optional.empty());
        when(generalRepository.save(any(UserProfileGeneral.class))).thenAnswer(inv -> inv.getArgument(0));
        when(socialRepository.findByProfileId(currentProfile.getId())).thenReturn(Optional.empty());
        when(socialRepository.save(any(UserProfileSocial.class))).thenAnswer(inv -> inv.getArgument(0));
        when(professionalRepository.findByProfileId(currentProfile.getId())).thenReturn(Optional.empty());
        when(professionalRepository.save(any(UserProfileProfessional.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personalRepository.findByProfileId(currentProfile.getId())).thenReturn(Optional.empty());
        when(personalRepository.save(any(UserProfilePersonal.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfileStatistics statistics = ProfileStatistics.builder()
            .user(currentUser)
            .scrapsCount(7L)
            .friendsCount(5L)
            .communitiesCount(3L)
            .testimonialsCount(2L)
            .fansCount(11L)
            .photosCount(13L)
            .build();
        when(profileStatisticsService.getOrCreateSnapshot(currentUser.getId())).thenReturn(statistics);
        when(profileFriendRepository.findOverviewByUserId(any(), any())).thenReturn(java.util.List.of());
        when(communityMembershipRepository.findOverviewByUserId(any(), any())).thenReturn(java.util.List.of());
        when(profileTestimonialRepository.findByAuthorIdOrderByCreatedAtDesc(currentUser.getId())).thenReturn(java.util.List.of());
        when(profileTestimonialRepository.findByTargetIdOrderByCreatedAtDesc(currentUser.getId())).thenReturn(java.util.List.of());
        when(profileTestimonialRepository.findByTargetIdOrderByCreatedAtDesc(currentUser.getId())).thenReturn(List.of());
        when(profileRatingRepository.findByTargetIdAndRaterId(currentUser.getId(), currentUser.getId())).thenReturn(Optional.empty());
        RatingSummaryProjection ratings = org.mockito.Mockito.mock(RatingSummaryProjection.class);
        when(ratings.getLegalPercentage()).thenReturn(88.5);
        when(ratings.getTrustworthyPercentage()).thenReturn(91.0);
        when(ratings.getSexyPercentage()).thenReturn(77.0);
        when(profileRatingRepository.findSummaryByTargetId(currentUser.getId())).thenReturn(ratings);

        ProfileOverviewDTO result = profileOverviewService.getOverview(null);

        assertThat(result.user().id()).isEqualTo(currentUser.getId());
        assertThat(result.user().name()).isEqualTo("Felipe Goulart");
        assertThat(result.general()).isNotNull();
        assertThat(result.social()).isNotNull();
        assertThat(result.professional()).isNotNull();
        assertThat(result.personal()).isNotNull();
        assertThat(result.shortcuts().scrapsCount()).isEqualTo(7L);
        assertThat(result.shortcuts().friendsCount()).isEqualTo(5L);
        assertThat(result.shortcuts().communitiesCount()).isEqualTo(3L);
        assertThat(result.shortcuts().testimonialsCount()).isEqualTo(2L);
        assertThat(result.ratings().legalPercentage()).isEqualTo(88.5);
        assertThat(result.testimonialsSent()).isEmpty();
        assertThat(result.testimonialsReceived()).isEmpty();

        verify(profileRepository).save(any(UserProfile.class));
        verify(generalRepository).save(any(UserProfileGeneral.class));
        verify(socialRepository).save(any(UserProfileSocial.class));
        verify(professionalRepository).save(any(UserProfileProfessional.class));
        verify(personalRepository).save(any(UserProfilePersonal.class));
    }

    @Test
    @DisplayName("View de outro usuário não deve criar registros novos")
    void shouldReadOtherUserWithoutCreatingRecords() {
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));
        when(profileRepository.findByUserId(otherUser.getId())).thenReturn(Optional.empty());
        ProfileStatistics statistics = ProfileStatistics.builder()
            .user(otherUser)
            .scrapsCount(3L)
            .friendsCount(0L)
            .communitiesCount(0L)
            .testimonialsCount(0L)
            .fansCount(0L)
            .photosCount(0L)
            .build();
        when(profileStatisticsService.getOrCreateSnapshot(otherUser.getId())).thenReturn(statistics);
        when(profileFriendRepository.findOverviewByUserId(any(), any())).thenReturn(java.util.List.of());
        when(communityMembershipRepository.findOverviewByUserId(any(), any())).thenReturn(java.util.List.of());
        when(profileTestimonialRepository.findByAuthorIdOrderByCreatedAtDesc(otherUser.getId())).thenReturn(java.util.List.of());
        when(profileTestimonialRepository.findByTargetIdAndStatusOrderByCreatedAtDesc(otherUser.getId(), TestimonialStatus.APPROVED)).thenReturn(List.of());
        when(profileRatingRepository.findByTargetIdAndRaterId(otherUser.getId(), currentUser.getId())).thenReturn(Optional.empty());
        RatingSummaryProjection ratings = org.mockito.Mockito.mock(RatingSummaryProjection.class);
        when(ratings.getLegalPercentage()).thenReturn(0D);
        when(ratings.getTrustworthyPercentage()).thenReturn(0D);
        when(ratings.getSexyPercentage()).thenReturn(0D);
        when(profileRatingRepository.findSummaryByTargetId(otherUser.getId())).thenReturn(ratings);

        ProfileOverviewDTO result = profileOverviewService.getOverview(otherUser.getId());

        assertThat(result.user().id()).isEqualTo(otherUser.getId());
        assertThat(result.general()).isNull();
        assertThat(result.social()).isNull();
        assertThat(result.professional()).isNull();
        assertThat(result.personal()).isNull();
        assertThat(result.shortcuts().scrapsCount()).isEqualTo(3L);

        verify(profileRepository, never()).save(any(UserProfile.class));
        verify(generalRepository, never()).save(any(UserProfileGeneral.class));
        verify(socialRepository, never()).save(any(UserProfileSocial.class));
        verify(professionalRepository, never()).save(any(UserProfileProfessional.class));
        verify(personalRepository, never()).save(any(UserProfilePersonal.class));
    }
}