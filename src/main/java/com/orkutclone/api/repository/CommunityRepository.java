package com.orkutclone.api.repository;

import com.orkutclone.api.model.Community;
import com.orkutclone.api.model.enums.CommunityCategory;
import com.orkutclone.api.repository.projection.CommunityListItemProjection;
import com.orkutclone.api.repository.projection.MyCommunityProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityRepository extends JpaRepository<Community, UUID> {

    /**
     * Loads the community together with its owner in a single query, avoiding a second
     * lazy-load round-trip for the owner when building the dashboard (open-in-view is disabled).
     * The join is a LEFT JOIN because an orphaned community (owner left) has no owner to fetch.
     */
    @Query("SELECT c FROM Community c LEFT JOIN FETCH c.owner WHERE c.id = :id")
    Optional<Community> findByIdWithOwner(@Param("id") UUID id);

    /**
     * Every community in a category, largest first. The member count and last-post date come
     * from scalar subqueries so the row stays a flat projection with no GROUP BY.
     */
    @Query("""
            SELECT c.id as id,
                   c.name as name,
                   c.icon as icon,
                   (SELECT COUNT(m) FROM CommunityMembership m
                     WHERE m.community = c AND m.status = 'APPROVED') as membersCount,
                   (SELECT MAX(msg.createdAt) FROM TopicMessage msg
                     WHERE msg.topic.community = c) as lastPostDate
            FROM Community c
            WHERE c.category = :category
            ORDER BY c.membersCount DESC, c.name ASC
            """)
    List<CommunityListItemProjection> findByCategory(@Param("category") CommunityCategory category, Pageable pageable);

    /** Case-insensitive "contains" search on the community name. */
    @Query("""
            SELECT c.id as id,
                   c.name as name,
                   c.icon as icon,
                   (SELECT COUNT(m) FROM CommunityMembership m
                     WHERE m.community = c AND m.status = 'APPROVED') as membersCount,
                   (SELECT MAX(msg.createdAt) FROM TopicMessage msg
                     WHERE msg.topic.community = c) as lastPostDate
            FROM Community c
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
            ORDER BY c.membersCount DESC, c.name ASC
            """)
    List<CommunityListItemProjection> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Communities the user owns, belongs to, or has a pending request for. The membership is
     * joined with an outer join so a community the user owns still shows up if its membership
     * row is somehow missing.
     */
    @Query("""
            SELECT c.id as id,
                   c.name as name,
                   c.icon as icon,
                   (SELECT COUNT(m2) FROM CommunityMembership m2
                     WHERE m2.community = c AND m2.status = 'APPROVED') as membersCount,
                   (SELECT MAX(msg.createdAt) FROM TopicMessage msg
                     WHERE msg.topic.community = c) as lastPostDate,
                   CASE WHEN c.owner.id = :userId THEN true ELSE false END as ownedByUser,
                   m.status as membershipStatus
            FROM Community c
            LEFT JOIN CommunityMembership m ON m.community = c AND m.user.id = :userId
            WHERE c.owner.id = :userId OR m.id IS NOT NULL
            ORDER BY c.name ASC
            """)
    List<MyCommunityProjection> findByUserInvolvement(@Param("userId") UUID userId);
}
