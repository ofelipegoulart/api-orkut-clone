package com.orkutclone.api.repository;

import com.orkutclone.api.model.UserProfileSocial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileSocialRepository extends JpaRepository<UserProfileSocial, UUID> {
    Optional<UserProfileSocial> findByProfileId(UUID profileId);

    @Query("SELECT s FROM UserProfileSocial s WHERE s.profile.user.id = :userId")
    Optional<UserProfileSocial> findByProfileUserId(@Param("userId") UUID userId);
}
