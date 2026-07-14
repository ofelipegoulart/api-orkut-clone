package com.orkutclone.api.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTopicRequest(
        @NotBlank @Size(max = 255) String title
) {}
