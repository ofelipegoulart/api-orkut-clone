package com.orkutclone.api.repository;

import com.orkutclone.api.model.UserProfileSocial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileSocialRepository extends JpaRepository<UserProfileSocial, UUID> {
    Optional<UserProfileSocial> findByProfileId(UUID profileId);
}
