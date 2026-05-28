package com.orkutclone.api.dto;

import java.time.LocalDate;

public record UpdateUserRequest(
        String name,
        String bio,
        String profilePicture,
        LocalDate birthDate
) {}
