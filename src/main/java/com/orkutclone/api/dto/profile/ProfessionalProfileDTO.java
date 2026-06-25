package com.orkutclone.api.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfessionalProfileDTO(
        @Schema(nullable = true) String education,
        @Schema(nullable = true) String school,
        @Schema(nullable = true) String college,
        @Schema(nullable = true) String course,
        @Schema(nullable = true) String degree,
        @Schema(nullable = true) String year,
        @Schema(nullable = true) String profession,
        @Schema(nullable = true) String sector,
        @Schema(nullable = true) String company,
        @Schema(nullable = true) String jobDescription,
        @Schema(nullable = true) String workPhone,
        @Schema(nullable = true) String professionalSkills,
        @Schema(nullable = true) String professionalInterests
) {}
