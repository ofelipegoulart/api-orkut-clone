package com.orkutclone.api.dto.album;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SetCoverRequest(
        @NotNull UUID photoId
) {}
