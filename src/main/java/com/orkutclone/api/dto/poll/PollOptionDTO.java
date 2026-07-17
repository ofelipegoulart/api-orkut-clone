package com.orkutclone.api.dto.poll;

import java.util.UUID;

public record PollOptionDTO(
        UUID id,
        String text,
        long voteCount
) {}
