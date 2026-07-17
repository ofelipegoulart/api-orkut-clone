package com.orkutclone.api.repository;

import com.orkutclone.api.model.Poll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PollRepository extends JpaRepository<Poll, UUID> {

    /** Fetches the creator in the same query to avoid an N+1 when rendering the list. */
    @Query(value = "SELECT p FROM Poll p JOIN FETCH p.creator WHERE p.community.id = :communityId ORDER BY p.createdAt DESC",
            countQuery = "SELECT COUNT(p) FROM Poll p WHERE p.community.id = :communityId")
    Page<Poll> findPageByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);

    /**
     * Loads the poll together with its community, the community's owner (needed for the
     * owner-only delete check) and the creator, avoiding extra round-trips (open-in-view is
     * disabled). The owner join is a LEFT JOIN because an orphaned community has none.
     */
    @Query("""
            SELECT p FROM Poll p
            JOIN FETCH p.creator
            JOIN FETCH p.community c
            LEFT JOIN FETCH c.owner
            WHERE p.id = :id AND p.community.id = :communityId
            """)
    Optional<Poll> findByIdAndCommunityId(@Param("id") UUID id, @Param("communityId") UUID communityId);

    /** The community's most recent poll, shown as the "active" one on the dashboard widget. */
    Optional<Poll> findFirstByCommunityIdOrderByCreatedAtDesc(UUID communityId);
}
