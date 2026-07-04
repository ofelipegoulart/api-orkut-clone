package com.orkutclone.api.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateProfileRatingRequest(
        @Min(1) @Max(6) Integer legal,
        @Min(1) @Max(6) Integer trustworthy,
        @Min(1) @Max(6) Integer sexy
) {}