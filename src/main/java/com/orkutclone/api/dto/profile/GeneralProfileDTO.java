package com.orkutclone.api.dto.profile;

import com.orkutclone.api.model.enums.PrivacyLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GeneralProfileDTO(
        @Schema(nullable = true) String firstName,
        @Schema(nullable = true) String lastName,
        @Schema(nullable = true) String gender,
        @Schema(nullable = true) String relationshipStatus,
        @Schema(nullable = true) String birthMonth,
        @Schema(nullable = true) String birthDay,
        @Schema(nullable = true) PrivacyLevel birthDatePrivacy,
        @Schema(nullable = true) String birthYear,
        @Schema(nullable = true) PrivacyLevel birthYearPrivacy,
        @Schema(nullable = true) String city,
        @Schema(nullable = true) String state,
        @Schema(nullable = true) String zipCode,
        @Schema(nullable = true) String country,
        List<String> languages,
        @Schema(nullable = true) PrivacyLevel languagesPrivacy,
        @Schema(nullable = true) String highSchool,
        @Schema(nullable = true) PrivacyLevel highSchoolPrivacy,
        @Schema(nullable = true) String college,
        @Schema(nullable = true) PrivacyLevel collegePrivacy,
        @Schema(nullable = true) String company,
        @Schema(nullable = true) PrivacyLevel companyPrivacy,
        List<String> interestedIn,
        @Schema(nullable = true) String datingPreference
) {}
