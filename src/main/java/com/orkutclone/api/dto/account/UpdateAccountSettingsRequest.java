package com.orkutclone.api.dto.account;

import com.orkutclone.api.model.enums.ContentFilterMode;
import com.orkutclone.api.model.enums.FriendUpdatesScope;
import com.orkutclone.api.model.enums.SlowInternetMode;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateAccountSettingsRequest(
        @NotNull String language,
        @NotNull Boolean birthdayReminders,
        @NotNull ContentFilterMode contentFilter,
        @NotNull FriendUpdatesScope friendUpdatesScope,
        @NotNull List<String> profileFeatures,
        @NotNull SlowInternetMode slowInternetMode,
        @NotNull Boolean suppressSlowInternetWarning
) {}
