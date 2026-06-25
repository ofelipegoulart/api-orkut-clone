package com.orkutclone.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Schema(minLength = 1) String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
) {}
