package com.orkutclone.api.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A community the current user is tied to, with the tie itself in {@code relation}. */
public record MyCommunityDTO(
        UUID id,
        String name,
        @Schema(nullable = true) String icon,

        @Schema(description = "Approved members only; pending join requests are not counted")
        long membersCount,

        @Schema(nullable = true) OffsetDateTime lastPostAt,

        @Schema(nullable = true, description = "Only the time (\"14:32\") when the last post was made today, the date (\"09/07/2026\") otherwise")
        String lastPostLabel,

        @Schema(description = "OWNER, MEMBER or PENDING", allowableValues = {"OWNER", "MEMBER", "PENDING"})
        String relation
) {}
