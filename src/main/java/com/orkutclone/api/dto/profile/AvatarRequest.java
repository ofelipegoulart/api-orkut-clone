package com.orkutclone.api.dto.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AvatarRequest(
        @NotBlank @Schema(minLength = 1) String avatar
) {}
