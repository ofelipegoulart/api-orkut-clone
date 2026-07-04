package com.orkutclone.api.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTestimonialRequest(
        @NotBlank @Size(max = 1024) String message
) {}