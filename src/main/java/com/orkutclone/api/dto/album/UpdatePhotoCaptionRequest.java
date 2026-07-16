package com.orkutclone.api.dto.album;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePhotoCaptionRequest(
        @Size(max = 500, message = "Caption must be at most 500 characters")
        @Pattern(regexp = "[^<>]*", message = "Caption must not contain HTML")
        String caption
) {}
