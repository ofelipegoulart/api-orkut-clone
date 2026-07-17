package com.orkutclone.api.dto.poll;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

/** A single poll row in a community's poll list. */
public record PollSummaryDTO(
        UUID id,
        UUID communityId,
        String question,
        @Schema(nullable = true) String imageUrl,
        UUID creatorId,
        String creatorName,
        @Schema(nullable = true) OffsetDateTime createdAt,
        @Schema(nullable = true) OffsetDateTime closesAt,
        boolean closed,
        boolean anonymous,
        boolean multipleChoice,
        long totalVotes,
        long commentsCount,
        @Schema(description = "Whether the authenticated user has already voted in this poll")
        boolean viewerVoted
) {}
