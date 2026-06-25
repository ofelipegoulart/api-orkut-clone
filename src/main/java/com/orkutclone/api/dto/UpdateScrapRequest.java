package com.orkutclone.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateScrapRequest(
        @NotBlank @Schema(minLength = 1) String content
) {}
