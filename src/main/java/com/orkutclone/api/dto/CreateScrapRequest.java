package com.orkutclone.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateScrapRequest(
        @NotBlank @Schema(minLength = 1) String content,
        @NotNull UUID ownerId,
        Boolean isPrivate,
        UUID parentId
) {}
