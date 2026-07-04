package com.orkutclone.api.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record CreateCommunityRequest(
        @NotBlank String name,
        String icon
) {}