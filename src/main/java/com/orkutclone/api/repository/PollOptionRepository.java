package com.orkutclone.api.repository;

import com.orkutclone.api.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {

    List<PollOption> findByPollIdOrderByOrderIndexAsc(UUID pollId);

    Optional<PollOption> findByIdAndPollId(UUID id, UUID pollId);

    void deleteByPollId(UUID pollId);
}
