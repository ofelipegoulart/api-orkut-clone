package com.orkutclone.api.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A pending join request awaiting the owner's decision, oldest first. */
public record JoinRequestDTO(
        UUID userId,
        String name,
        @Schema(nullable = true) String photoUrl,
        @Schema(nullable = true) OffsetDateTime requestedAt
) {}
