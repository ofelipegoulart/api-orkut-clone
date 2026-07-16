package com.orkutclone.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank String password,
        @AssertTrue(message = "confirm must be true to delete the account") boolean confirm
) {}
