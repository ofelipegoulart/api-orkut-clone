package com.orkutclone.api.dto.profile;

import com.orkutclone.api.model.enums.PrivacyLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record SocialProfileDTO(
        @Schema(nullable = true) String children,
        @Schema(nullable = true) String ethnicity,
        @Schema(nullable = true) String religion,
        @Schema(nullable = true) String politicalView,
        @Schema(nullable = true) String sexualOrientation,
        @Schema(nullable = true) PrivacyLevel sexualOrientationPrivacy,
        List<String> humor,
        List<String> style,
        @Schema(nullable = true) String smoking,
        @Schema(nullable = true) String drinking,
        @Schema(nullable = true) String pets,
        @Schema(nullable = true) String livingWith,
        @Schema(nullable = true) String hometown,
        @Schema(nullable = true) String website,
        @Schema(nullable = true) String aboutMe,
        @Schema(nullable = true) String passions,
        @Schema(nullable = true) String sports,
        @Schema(nullable = true) String activities,
        @Schema(nullable = true) String books,
        @Schema(nullable = true) String music,
        @Schema(nullable = true) String tvShows,
        @Schema(nullable = true) String movies,
        @Schema(nullable = true) String cuisines
) {}
