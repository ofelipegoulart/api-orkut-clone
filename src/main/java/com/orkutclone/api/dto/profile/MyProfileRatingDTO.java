package com.orkutclone.api.dto.profile;

/**
 * Which categories the authenticated user has already rated on a given profile.
 * Each flag is {@code true} once the current user has submitted a value for that category.
 */
public record MyProfileRatingDTO(
        boolean legal,
        boolean trustworthy,
        boolean sexy
) {}
