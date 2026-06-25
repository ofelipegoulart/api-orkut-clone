package com.orkutclone.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        @Schema(nullable = true) String bio,
        @Schema(nullable = true) String profilePicture,
        @Schema(nullable = true) String avatar,
        @Schema(nullable = true) LocalDate birthDate,
        OffsetDateTime createdAt
) {}
