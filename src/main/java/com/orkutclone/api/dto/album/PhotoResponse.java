package com.orkutclone.api.dto.album;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PhotoResponse(
        UUID id,
        UUID albumId,
        String url,
        @Schema(nullable = true) String caption,
        int orderIndex,
        OffsetDateTime createdAt
) {}
