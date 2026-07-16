package com.orkutclone.api.dto.album;

import java.util.List;

public record AlbumsPageDTO(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<AlbumResponse> results
) {}
