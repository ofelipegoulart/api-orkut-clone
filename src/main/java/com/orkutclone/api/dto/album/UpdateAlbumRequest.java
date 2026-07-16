package com.orkutclone.api.dto.album;

import com.orkutclone.api.model.enums.AlbumPrivacy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAlbumRequest(
        @NotBlank @Size(max = 120) String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        @Pattern(regexp = "[^<>]*", message = "Description must not contain HTML")
        String description,

        @NotNull AlbumPrivacy privacy
) {}
