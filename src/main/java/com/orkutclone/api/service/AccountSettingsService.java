package com.orkutclone.api.service;

import com.orkutclone.api.dto.account.AccountSettingsDTO;
import com.orkutclone.api.dto.account.UpdateAccountSettingsRequest;
import com.orkutclone.api.model.AccountSettings;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.AccountSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class AccountSettingsService {

    private final AccountSettingsRepository accountSettingsRepository;

    @Transactional
    public AccountSettingsDTO getSettings() {
        return toDTO(getOrCreate(authenticatedUser()));
    }

    @Transactional
    public AccountSettingsDTO updateSettings(UpdateAccountSettingsRequest request) {
        AccountSettings settings = getOrCreate(authenticatedUser());

        settings.setLanguage(request.language());
        settings.setBirthdayReminders(request.birthdayReminders());
        settings.setContentFilter(request.contentFilter());
        settings.setFriendUpdatesScope(request.friendUpdatesScope());
        settings.setProfileFeatures(new ArrayList<>(request.profileFeatures()));
        settings.setSlowInternetMode(request.slowInternetMode());
        settings.setSuppressSlowInternetWarning(request.suppressSlowInternetWarning());

        return toDTO(accountSettingsRepository.save(settings));
    }

    private AccountSettings getOrCreate(User user) {
        return accountSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    AccountSettings settings = new AccountSettings();
                    settings.setUser(user);
                    return accountSettingsRepository.save(settings);
                });
    }

    private AccountSettingsDTO toDTO(AccountSettings settings) {
        return new AccountSettingsDTO(
                settings.getLanguage(),
                settings.isBirthdayReminders(),
                settings.getContentFilter(),
                settings.getFriendUpdatesScope(),
                settings.getProfileFeatures(),
                settings.getSlowInternetMode(),
                settings.isSuppressSlowInternetWarning());
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
