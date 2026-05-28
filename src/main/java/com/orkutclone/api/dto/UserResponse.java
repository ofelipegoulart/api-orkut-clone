package com.orkutclone.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String bio,
        String profilePicture,
        LocalDate birthDate,
        LocalDateTime createdAt
) {}
