package com.orkutclone.api.repository;

import com.orkutclone.api.model.UserProfileGeneral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileGeneralRepository extends JpaRepository<UserProfileGeneral, UUID> {
    Optional<UserProfileGeneral> findByProfileId(UUID profileId);

    @Query("SELECT g FROM UserProfileGeneral g WHERE g.profile.user.id = :userId")
    Optional<UserProfileGeneral> findByProfileUserId(@Param("userId") UUID userId);
}
