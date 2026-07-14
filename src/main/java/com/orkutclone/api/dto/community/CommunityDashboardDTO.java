package com.orkutclone.api.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Consolidated payload to render a community home page in a single request:
 * community basics, forum topics, the active poll (if any) and the featured members grid.
 */
public record CommunityDashboardDTO(
        CommunityInfo community,
        List<TopicBriefDTO> topics,
        @Schema(nullable = true) ActivePollDTO activePoll,
        List<MemberSummaryDTO> featuredMembers
) {

    public record CommunityInfo(
            UUID id,
            String name,
            @Schema(nullable = true) String description,
            @Schema(nullable = true) String icon,
            @Schema(nullable = true) String language,
            @Schema(nullable = true) String category,
            @Schema(nullable = true) String categoryLabel,
            @Schema(nullable = true) UUID ownerId,
            @Schema(nullable = true) String ownerName,
            String type,
            String contentPrivacy,
            @Schema(nullable = true) LocationDTO location,
            CommunityDetailDTO.FeatureSettingsDTO features,
            long membersCount,
            @Schema(nullable = true) OffsetDateTime createdAt
    ) {}

    public record LocationDTO(
            @Schema(nullable = true) String city,
            @Schema(nullable = true) String state,
            @Schema(nullable = true) String zipCode,
            @Schema(nullable = true) String country
    ) {}

    public record TopicBriefDTO(
            UUID id,
            String title,
            long totalPosts,
            @Schema(nullable = true) OffsetDateTime lastPostDate
    ) {}

    public record ActivePollDTO(
            UUID id,
            String question,
            @Schema(nullable = true) String creator,
            List<PollOptionDTO> voteOptions
    ) {}

    public record PollOptionDTO(
            UUID id,
            String text,
            long voteCount
    ) {}

    public record MemberSummaryDTO(
            UUID id,
            String name,
            @Schema(nullable = true) String photoUrl,
            long friendsCount
    ) {}
}
