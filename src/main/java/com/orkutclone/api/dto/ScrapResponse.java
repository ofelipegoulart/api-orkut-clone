package com.orkutclone.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ScrapResponse(
        UUID id,
        String content,
        boolean isPrivate,
        UUID authorId,
        String authorName,
        UUID ownerId,
        String ownerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
