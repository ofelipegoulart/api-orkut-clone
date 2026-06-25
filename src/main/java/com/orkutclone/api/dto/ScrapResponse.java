package com.orkutclone.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScrapResponse(
        UUID id,
        String content,
        boolean isPrivate,
        UUID authorId,
        String authorName,
        String authorAvatar,
        UUID ownerId,
        String ownerName,
        UUID parentId,
        OffsetDateTime readAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
