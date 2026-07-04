package com.orkutclone.api.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProfileOverviewDTO(
        UserSummary user,
        GeneralProfileDTO general,
        SocialProfileDTO social,
        ProfessionalProfileDTO professional,
        PersonalProfileDTO personal,
        List<FriendCardDTO> friends,
        List<CommunityCardDTO> communities,
        List<TestimonialDTO> testimonialsSent,
        List<TestimonialDTO> testimonialsReceived,
        ShortcutsDTO shortcuts,
        RatingsDTO ratings
) {

    public record UserSummary(
            UUID id,
            String name,
            @Schema(nullable = true) String avatar
    ) {}

    public record FriendCardDTO(
            UUID id,
            String name,
            @Schema(nullable = true) String avatar,
            long friendsCount,
            long mutualFriendsCount
    ) {}

    public record CommunityCardDTO(
            UUID id,
            String name,
            @Schema(nullable = true) String icon
    ) {}

    public record TestimonialDTO(
            UUID id,
            UUID authorId,
            String authorName,
            @Schema(nullable = true) String authorAvatar,
            String message,
            @Schema(nullable = true) String status,
            @Schema(nullable = true) OffsetDateTime createdAt
    ) {}

    public record ShortcutsDTO(
            long scrapsCount,
            long friendsCount,
            long communitiesCount,
            long testimonialsCount,
            long photosCount,
            long fansCount
    ) {}

    public record RatingsDTO(
            double legalPercentage,
            double trustworthyPercentage,
            double sexyPercentage
    ) {}
}