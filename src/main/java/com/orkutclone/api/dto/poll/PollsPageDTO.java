package com.orkutclone.api.dto.poll;

import java.util.List;

public record PollsPageDTO(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<PollSummaryDTO> results
) {}
