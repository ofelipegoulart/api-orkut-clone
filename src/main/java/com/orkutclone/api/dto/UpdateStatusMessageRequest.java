package com.orkutclone.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateStatusMessageRequest(
        @Size(max = 140, message = "Status message exceeds maximum length of 140 characters") String statusMessage
) {}
