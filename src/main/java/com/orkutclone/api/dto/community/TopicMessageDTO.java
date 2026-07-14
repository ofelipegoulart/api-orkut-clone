package com.orkutclone.api.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A single message row in a topic's message list. */
public record TopicMessageDTO(
        UUID id,
        UUID authorId,
        String authorName,
        @Schema(nullable = true) String authorAvatar,
        String subject,
        String message,
        @Schema(nullable = true) OffsetDateTime createdAt,
        @Schema(description = "Human-friendly relative time, e.g. \"2 dias atrás\" or \"14 jul 2026\"")
        String createdAtLabel
) {}
