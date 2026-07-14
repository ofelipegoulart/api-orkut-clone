package com.orkutclone.api.dto.community;

import java.util.List;
import java.util.UUID;

/**
 * One page of a topic's messages plus the header context (community and topic names)
 * and the pagination metadata the UI needs for first / previous / next / last controls.
 */
public record TopicMessagesPageDTO(
        UUID topicId,
        String topicTitle,
        UUID communityId,
        String communityName,
        int page,
        int size,
        long totalMessages,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious,
        List<TopicMessageDTO> messages
) {}
