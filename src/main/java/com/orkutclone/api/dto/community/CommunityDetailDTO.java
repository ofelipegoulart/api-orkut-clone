package com.orkutclone.api.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A community as returned right after creation. */
public record CommunityDetailDTO(
        UUID id,
        String name,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) String icon,
        @Schema(nullable = true) String language,
        String category,
        String categoryLabel,
        String type,
        String contentPrivacy,
        @Schema(nullable = true) CommunityDashboardDTO.LocationDTO location,
        FeatureSettingsDTO features,
        UUID ownerId,
        long membersCount,
        @Schema(nullable = true) OffsetDateTime createdAt
) {

    public record FeatureSettingsDTO(
            boolean forumEnabled,
            boolean forumOnHomepage,
            boolean forumNoAnonymousPosts,
            boolean pollsEnabled,
            boolean pollsOnHomepage,
            boolean eventsEnabled,
            boolean eventsOnHomepage,
            boolean customNewsEnabled
    ) {}
}
