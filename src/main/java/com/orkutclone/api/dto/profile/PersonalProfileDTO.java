package com.orkutclone.api.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PersonalProfileDTO(
        @Schema(nullable = true) String eyeColor,
        @Schema(nullable = true) String hairColor,
        @Schema(nullable = true) String height,
        @Schema(nullable = true) String bodyType,
        @Schema(nullable = true) String appearance,
        @Schema(nullable = true) String bodyArt,
        @Schema(nullable = true) String perfectMatch,
        List<String> attractions,
        @Schema(nullable = true) String cantStand,
        @Schema(nullable = true) String idealFirstDate,
        @Schema(nullable = true) String pastRelationshipsLessons,
        @Schema(nullable = true) String whatStandsOut,
        @Schema(nullable = true) String favoriteBodyPart,
        @Schema(nullable = true) String fiveEssentials,
        @Schema(nullable = true) String inMyRoom
) {}
