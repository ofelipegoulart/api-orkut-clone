package com.orkutclone.api.repository;

import com.orkutclone.api.model.ProfileFriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfileFriendRequestRepository extends JpaRepository<ProfileFriendRequest, UUID> {

    boolean existsByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId);

    Optional<ProfileFriendRequest> findByRequesterIdAndReceiverId(UUID requesterId, UUID receiverId);

    Optional<ProfileFriendRequest> findByIdAndReceiverId(UUID id, UUID receiverId);

    Optional<ProfileFriendRequest> findByIdAndRequesterId(UUID id, UUID requesterId);

    List<ProfileFriendRequest> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId);

    List<ProfileFriendRequest> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
}
