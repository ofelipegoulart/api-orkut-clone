package com.orkutclone.api.repository;

import com.orkutclone.api.model.UserProfileProfessional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileProfessionalRepository extends JpaRepository<UserProfileProfessional, UUID> {
    Optional<UserProfileProfessional> findByProfileId(UUID profileId);
}
