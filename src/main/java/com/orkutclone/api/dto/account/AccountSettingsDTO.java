package com.orkutclone.api.dto.account;

import com.orkutclone.api.model.enums.ContentFilterMode;
import com.orkutclone.api.model.enums.FriendUpdatesScope;
import com.orkutclone.api.model.enums.SlowInternetMode;

import java.util.List;

public record AccountSettingsDTO(
        String language,
        boolean birthdayReminders,
        ContentFilterMode contentFilter,
        FriendUpdatesScope friendUpdatesScope,
        List<String> profileFeatures,
        SlowInternetMode slowInternetMode,
        boolean suppressSlowInternetWarning
) {}
