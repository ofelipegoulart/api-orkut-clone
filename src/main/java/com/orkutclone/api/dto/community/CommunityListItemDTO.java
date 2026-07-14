package com.orkutclone.api.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A community card in a category listing or a search result. */
public record CommunityListItemDTO(
        UUID id,
        String name,
        @Schema(nullable = true) String icon,

        @Schema(description = "Approved members only; pending join requests are not counted")
        long membersCount,

        @Schema(nullable = true, description = "Date of the community's most recent forum post")
        OffsetDateTime lastPostAt,

        @Schema(nullable = true, description = "Ready-to-render label: only the time (\"14:32\") when the post was made today, the date (\"09/07/2026\") otherwise")
        String lastPostLabel
) {}
