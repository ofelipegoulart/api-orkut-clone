package com.orkutclone.api.repository;

import com.orkutclone.api.model.ProfileStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileStatisticsRepository extends JpaRepository<ProfileStatistics, UUID> {
    Optional<ProfileStatistics> findByUserId(UUID userId);
}