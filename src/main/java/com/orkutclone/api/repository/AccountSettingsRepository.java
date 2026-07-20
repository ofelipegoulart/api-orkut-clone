package com.orkutclone.api.repository;

import com.orkutclone.api.model.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountSettingsRepository extends JpaRepository<AccountSettings, UUID> {

    Optional<AccountSettings> findByUserId(UUID userId);
}
