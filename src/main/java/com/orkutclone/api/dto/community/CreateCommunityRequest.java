package com.orkutclone.api.dto.community;

import com.orkutclone.api.model.enums.CommunityCategory;
import com.orkutclone.api.model.enums.CommunityContentPrivacy;
import com.orkutclone.api.model.enums.CommunityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload of the "Criar Comunidade" form.
 *
 * <p>The icon is sent as a URL: upload the image first through
 * {@code POST /api/community/icon} and send the URL it returns here.</p>
 */
public record CreateCommunityRequest(

        @NotBlank @Size(max = 120) String name,

        @NotNull CommunityCategory category,

        @NotNull CommunityType type,

        @NotNull CommunityContentPrivacy contentPrivacy,

        @Schema(defaultValue = "Português")
        @Size(max = 60) String language,

        @Valid LocationRequest location,

        @Schema(description = "Public URL returned by the icon upload endpoint")
        @Size(max = 512) String icon,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        @Pattern(regexp = "[^<>]*", message = "Description must not contain HTML")
        String description,

        @Schema(description = "Omit to accept the defaults: forum and polls on, events on but hidden from the home page, custom news off")
        @Valid FeatureSettingsRequest features
) {

    public record LocationRequest(
            @Size(max = 120) String city,
            @Size(max = 120) String state,
            @Size(max = 20) String zipCode,
            @Size(max = 120) String country
    ) {}

    /** Every flag is optional; a null falls back to the default the form pre-selects. */
    public record FeatureSettingsRequest(
            @Schema(defaultValue = "true") Boolean forumEnabled,
            @Schema(defaultValue = "true") Boolean forumOnHomepage,
            @Schema(defaultValue = "true") Boolean forumNoAnonymousPosts,
            @Schema(defaultValue = "true") Boolean pollsEnabled,
            @Schema(defaultValue = "true") Boolean pollsOnHomepage,
            @Schema(defaultValue = "true") Boolean eventsEnabled,
            @Schema(defaultValue = "false") Boolean eventsOnHomepage,
            @Schema(defaultValue = "false") Boolean customNewsEnabled
    ) {}
}
