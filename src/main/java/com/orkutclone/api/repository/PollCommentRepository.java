package com.orkutclone.api.repository;

import com.orkutclone.api.model.PollComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PollCommentRepository extends JpaRepository<PollComment, UUID> {

    /** Fetches the author in the same query to avoid an N+1 when rendering the comment list. */
    @Query("SELECT c FROM PollComment c JOIN FETCH c.author WHERE c.poll.id = :pollId ORDER BY c.createdAt ASC")
    List<PollComment> findByPollIdOrderByCreatedAtAsc(@Param("pollId") UUID pollId);

    long countByPollId(UUID pollId);

    void deleteByPollId(UUID pollId);
}
