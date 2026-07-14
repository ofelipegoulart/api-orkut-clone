package com.orkutclone.api.repository;

import com.orkutclone.api.model.CommunityTopic;
import com.orkutclone.api.repository.projection.CommunityTopicBriefProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityTopicRepository extends JpaRepository<CommunityTopic, UUID> {

    /** Loads the topic together with its community so the header (community name) needs no extra round-trip. */
    @Query("SELECT t FROM CommunityTopic t JOIN FETCH t.community WHERE t.id = :id")
    Optional<CommunityTopic> findByIdWithCommunity(@Param("id") UUID id);

    /**
     * Brief of each topic in a community with its message total and last-post date,
     * for the dashboard forum list. Aggregates are computed via scalar subqueries to
     * avoid a GROUP BY and keep the mapping to a simple projection.
     */
    @Query("""
            SELECT t.id as id,
                   t.title as title,
                   (SELECT COUNT(m) FROM TopicMessage m WHERE m.topic = t) as totalPosts,
                   (SELECT MAX(m.createdAt) FROM TopicMessage m WHERE m.topic = t) as lastPostDate
            FROM CommunityTopic t
            WHERE t.community.id = :communityId
            ORDER BY t.createdAt DESC
            """)
    List<CommunityTopicBriefProjection> findBriefsByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);
}
