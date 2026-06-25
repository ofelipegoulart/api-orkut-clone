package com.orkutclone.api.repository;

import com.orkutclone.api.model.UserProfilePersonal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfilePersonalRepository extends JpaRepository<UserProfilePersonal, UUID> {
    Optional<UserProfilePersonal> findByProfileId(UUID profileId);
}
