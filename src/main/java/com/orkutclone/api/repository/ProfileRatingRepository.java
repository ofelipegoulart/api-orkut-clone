package com.orkutclone.api.repository;

import com.orkutclone.api.model.ProfileRating;
import com.orkutclone.api.repository.projection.RatingSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProfileRatingRepository extends JpaRepository<ProfileRating, UUID> {

    java.util.Optional<ProfileRating> findByTargetIdAndRaterId(UUID targetId, UUID raterId);

    @Query("""
            SELECT COALESCE(AVG(r.legalPercentage), 0) as legalPercentage,
                   COALESCE(AVG(r.trustworthyPercentage), 0) as trustworthyPercentage,
                   COALESCE(AVG(r.sexyPercentage), 0) as sexyPercentage
            FROM ProfileRating r
            WHERE r.target.id = :targetId
            """)
    RatingSummaryProjection findSummaryByTargetId(@Param("targetId") UUID targetId);
}