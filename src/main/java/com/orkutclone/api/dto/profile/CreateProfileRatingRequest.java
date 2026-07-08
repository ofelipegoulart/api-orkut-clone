package com.orkutclone.api.dto.profile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Rating for a profile. Each category (1-6) is optional so a user may rate just one, two,
 * or all three. A category left {@code null} keeps its previous value; at least one is required.
 */
public record CreateProfileRatingRequest(
        @Min(1) @Max(6) Integer legal,
        @Min(1) @Max(6) Integer trustworthy,
        @Min(1) @Max(6) Integer sexy
) {}