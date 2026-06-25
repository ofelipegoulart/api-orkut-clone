package com.orkutclone.api.repository;

import com.orkutclone.api.model.UserProfileGeneral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileGeneralRepository extends JpaRepository<UserProfileGeneral, UUID> {
    Optional<UserProfileGeneral> findByProfileId(UUID profileId);
}
