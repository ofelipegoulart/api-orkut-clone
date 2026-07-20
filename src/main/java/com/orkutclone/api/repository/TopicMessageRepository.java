package com.orkutclone.api.repository;

import com.orkutclone.api.model.TopicMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicMessageRepository extends JpaRepository<TopicMessage, UUID> {

    /**
     * One page of a topic's messages, oldest first, fetching the author in the same query
     * to avoid an N+1 when rendering the list (open-in-view is disabled).
     */
    @Query(value = """
            SELECT m FROM TopicMessage m
            JOIN FETCH m.author
            WHERE m.topic.id = :topicId
            ORDER BY m.createdAt ASC
            """,
            countQuery = "SELECT COUNT(m) FROM TopicMessage m WHERE m.topic.id = :topicId")
    Page<TopicMessage> findPageByTopicId(@Param("topicId") UUID topicId, Pageable pageable);

    long countByTopicId(UUID topicId);

    Optional<TopicMessage> findFirstByTopicIdOrderByCreatedAtDesc(UUID topicId);

    /** All of a topic's messages, oldest first — used to locate the one matching a search term. */
    List<TopicMessage> findByTopicIdOrderByCreatedAtAsc(UUID topicId);
}
