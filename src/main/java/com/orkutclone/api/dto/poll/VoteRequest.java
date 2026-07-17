package com.orkutclone.api.dto.poll;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * The option(s) a voter checked in a single vote. A single-choice poll rejects more than one
 * id; a multiple-choice poll accepts several.
 */
public record VoteRequest(
        @NotEmpty(message = "Pick at least one option")
        List<@NotNull UUID> optionIds
) {}
