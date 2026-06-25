package com.orkutclone.api.dto.profile;

import com.orkutclone.api.model.enums.PrivacyLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ContactProfileDTO(
        @Schema(nullable = true) String primaryEmail,
        @Schema(nullable = true) PrivacyLevel primaryEmailPrivacy,
        List<SecondaryEmailDTO> secondaryEmails,
        @Schema(nullable = true) String im1,
        @Schema(nullable = true) PrivacyLevel im1Privacy,
        @Schema(nullable = true) String im2,
        @Schema(nullable = true) PrivacyLevel im2Privacy,
        @Schema(nullable = true) String homePhone,
        @Schema(nullable = true) PrivacyLevel homePhonePrivacy,
        @Schema(nullable = true) String mobilePhone,
        @Schema(nullable = true) PrivacyLevel mobilePhonePrivacy,
        @Schema(nullable = true) String address1,
        @Schema(nullable = true) String address2,
        @Schema(nullable = true) String addressCity,
        @Schema(nullable = true) String addressState,
        @Schema(nullable = true) String addressZipCode,
        @Schema(nullable = true) String addressCountry
) {}
