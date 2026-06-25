package com.orkutclone.api.repository;

import com.orkutclone.api.model.UserProfileContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileContactRepository extends JpaRepository<UserProfileContact, UUID> {
    Optional<UserProfileContact> findByProfileId(UUID profileId);
}
