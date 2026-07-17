package com.orkutclone.api.dto.poll;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePollCommentRequest(
        @NotBlank
        @Size(max = 500, message = "Comment must be at most 500 characters")
        @Pattern(regexp = "[^<>]*", message = "Comment must not contain HTML")
        String message
) {}
