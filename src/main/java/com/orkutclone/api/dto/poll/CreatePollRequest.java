package com.orkutclone.api.dto.poll;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public record CreatePollRequest(
        @NotBlank
        @Size(max = 280, message = "Question must be at most 280 characters")
        @Pattern(regexp = "[^<>]*", message = "Question must not contain HTML")
        String question,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        @Pattern(regexp = "[^<>]*", message = "Description must not contain HTML")
        String description,

        @Schema(description = "Public URL returned by the poll image upload endpoint")
        @Size(max = 512) String imageUrl,

        @NotNull
        @Size(min = 2, max = 10, message = "A poll needs between 2 and 10 options")
        List<
                @NotBlank
                @Size(max = 140, message = "Option must be at most 140 characters")
                @Pattern(regexp = "[^<>]*", message = "Option must not contain HTML")
                String
        > options,

        @Schema(description = "When the poll stops accepting votes; omit for a poll that never closes on its own")
        @Future(message = "Closing date must be in the future")
        OffsetDateTime closesAt,

        @Schema(description = "When true, votes are never attributed to a user (e.g. no tag on comments); defaults to false", defaultValue = "false")
        Boolean anonymous,

        @Schema(description = "When true, a voter may check more than one option; when false they must pick exactly one. Defaults to false", defaultValue = "false")
        Boolean multipleChoice
) {}
