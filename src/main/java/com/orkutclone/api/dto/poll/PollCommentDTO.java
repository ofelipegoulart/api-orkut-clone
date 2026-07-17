package com.orkutclone.api.dto.poll;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PollCommentDTO(
        UUID id,
        UUID authorId,
        String authorName,
        @Schema(nullable = true) String authorAvatar,
        String message,
        @Schema(nullable = true) OffsetDateTime createdAt,
        @Schema(description = "Human-friendly relative time, e.g. \"2 dias atrás\" or \"14 jul 2026\"")
        String createdAtLabel,
        @Schema(description = "The option(s) the author voted for, when the poll isn't anonymous and they voted; empty otherwise")
        List<UUID> votedOptionIds,
        List<String> votedOptionTexts
) {}
