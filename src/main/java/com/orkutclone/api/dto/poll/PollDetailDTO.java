package com.orkutclone.api.dto.poll;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PollDetailDTO(
        UUID id,
        UUID communityId,
        String communityName,
        String question,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) String imageUrl,
        UUID creatorId,
        String creatorName,
        @Schema(nullable = true) OffsetDateTime createdAt,
        @Schema(nullable = true) OffsetDateTime closesAt,
        boolean closed,
        boolean anonymous,
        @Schema(description = "Whether a voter may check more than one option in their single vote")
        boolean multipleChoice,
        List<PollOptionDTO> options,
        long totalVotes,
        @Schema(description = "The option(s) the authenticated user voted for; empty if they haven't voted")
        List<UUID> viewerVoteOptionIds,
        List<PollCommentDTO> comments
) {}
