package com.orkutclone.api.repository;

import com.orkutclone.api.model.PollVote;
import com.orkutclone.api.repository.projection.PollOptionVoteCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    boolean existsByPollIdAndVoterId(UUID pollId, UUID voterId);

    /** All the options a voter checked in their single vote — more than one for a multiple-choice poll. */
    List<PollVote> findAllByPollIdAndVoterId(UUID pollId, UUID voterId);

    long countByPollId(UUID pollId);

    void deleteByPollId(UUID pollId);

    @Query("SELECT v.option.id as optionId, COUNT(v) as voteCount FROM PollVote v WHERE v.poll.id = :pollId GROUP BY v.option.id")
    List<PollOptionVoteCountProjection> countByOptionForPoll(@Param("pollId") UUID pollId);
}
