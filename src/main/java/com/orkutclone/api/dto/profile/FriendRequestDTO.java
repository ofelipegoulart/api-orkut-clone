package com.orkutclone.api.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FriendRequestDTO(
        UUID requestId,
        UUID userId,
        String name,
        @Schema(nullable = true) String avatar,
        @Schema(nullable = true) OffsetDateTime createdAt
) {}
