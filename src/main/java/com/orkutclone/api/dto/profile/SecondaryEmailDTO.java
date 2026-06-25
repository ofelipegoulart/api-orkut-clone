package com.orkutclone.api.dto.profile;

import com.orkutclone.api.model.enums.PrivacyLevel;

public record SecondaryEmailDTO(String email, PrivacyLevel privacy) {}
