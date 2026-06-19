package com.orkutclone.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateScrapRequest(
        @NotBlank String content
) {}
