package com.orkutclone.api.dto.community;

import java.util.UUID;

/** Lightweight topic header, returned when a topic is created. */
public record TopicSummaryDTO(
        UUID id,
        String title,
        UUID communityId,
        String communityName,
        long totalMessages
) {}
