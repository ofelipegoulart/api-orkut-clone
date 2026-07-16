package com.orkutclone.api.dto.album;

import com.orkutclone.api.model.enums.AlbumPrivacy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AlbumDetailDTO(
        UUID id,
        UUID ownerId,
        String title,
        @Schema(nullable = true) String description,
        AlbumPrivacy privacy,
        @Schema(nullable = true) String coverPhotoUrl,
        long photoCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PhotoResponse> photos
) {}
