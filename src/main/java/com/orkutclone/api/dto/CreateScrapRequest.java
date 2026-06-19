package com.orkutclone.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateScrapRequest(
        @NotBlank String content,
        @NotNull UUID ownerId,
        Boolean isPrivate
) {}
