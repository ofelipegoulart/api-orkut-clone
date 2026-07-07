package com.orkutclone.api.repository;

import com.orkutclone.api.model.CommunityMembership;
import com.orkutclone.api.repository.projection.CommunityOverviewProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, UUID> {

    @Query("SELECT COUNT(m) FROM CommunityMembership m WHERE m.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM CommunityMembership m WHERE m.community.id = :communityId")
    long countByCommunityId(@Param("communityId") UUID communityId);

    boolean existsByUserIdAndCommunityId(UUID userId, UUID communityId);

    void deleteByUserIdAndCommunityId(UUID userId, UUID communityId);

    @Query("""
            SELECT c.id as id,
                   c.name as name,
                   c.icon as icon
            FROM CommunityMembership m
            JOIN m.community c
            WHERE m.user.id = :userId
            ORDER BY m.createdAt DESC
            """)
    List<CommunityOverviewProjection> findOverviewByUserId(@Param("userId") UUID userId, Pageable pageable);
}