package com.orkutclone.api.repository;

import com.orkutclone.api.model.CommunityMembership;
import com.orkutclone.api.model.enums.MembershipStatus;
import com.orkutclone.api.repository.projection.CommunityJoinRequestProjection;
import com.orkutclone.api.repository.projection.CommunityMemberProjection;
import com.orkutclone.api.repository.projection.CommunityOverviewProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Only {@code APPROVED} rows are effective memberships; every count and listing below
 * filters on that so pending join requests never leak into member lists or counters.
 */
public interface CommunityMembershipRepository extends JpaRepository<CommunityMembership, UUID> {

    @Query("SELECT COUNT(m) FROM CommunityMembership m WHERE m.user.id = :userId AND m.status = 'APPROVED'")
    long countByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM CommunityMembership m WHERE m.community.id = :communityId AND m.status = 'APPROVED'")
    long countByCommunityId(@Param("communityId") UUID communityId);

    /** True for both pending and approved rows — used to reject duplicate join requests. */
    boolean existsByUserIdAndCommunityId(UUID userId, UUID communityId);

    boolean existsByUserIdAndCommunityIdAndStatus(UUID userId, UUID communityId, MembershipStatus status);

    Optional<CommunityMembership> findByUserIdAndCommunityId(UUID userId, UUID communityId);

    void deleteByUserIdAndCommunityId(UUID userId, UUID communityId);

    @Query("""
            SELECT c.id as id,
                   c.name as name,
                   c.icon as icon,
                   (SELECT COUNT(m2) FROM CommunityMembership m2 WHERE m2.community.id = c.id AND m2.status = 'APPROVED') as memberCount
            FROM CommunityMembership m
            JOIN m.community c
            WHERE m.user.id = :userId AND m.status = 'APPROVED'
            ORDER BY m.createdAt DESC
            """)
    List<CommunityOverviewProjection> findOverviewByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT u.id as id,
                   u.name as name,
                   u.profilePicture as avatar,
                   (SELECT COUNT(f) FROM ProfileFriend f WHERE f.user.id = u.id) as friendsCount
            FROM CommunityMembership m
            JOIN m.user u
            WHERE m.community.id = :communityId AND m.status = 'APPROVED'
            ORDER BY m.createdAt DESC
            """)
    List<CommunityMemberProjection> findMembersByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);

    @Query("""
            SELECT u.id as userId,
                   u.name as name,
                   u.profilePicture as avatar,
                   m.createdAt as requestedAt
            FROM CommunityMembership m
            JOIN m.user u
            WHERE m.community.id = :communityId AND m.status = 'PENDING'
            ORDER BY m.createdAt ASC
            """)
    List<CommunityJoinRequestProjection> findPendingRequestsByCommunityId(@Param("communityId") UUID communityId);
}
