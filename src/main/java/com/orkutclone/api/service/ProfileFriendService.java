package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.FriendRequestDTO;
import com.orkutclone.api.model.ProfileFriend;
import com.orkutclone.api.model.ProfileFriendRequest;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.ProfileFriendRepository;
import com.orkutclone.api.repository.ProfileFriendRequestRepository;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileFriendService {

    private final UserRepository userRepository;
    private final ProfileFriendRepository profileFriendRepository;
    private final ProfileFriendRequestRepository friendRequestRepository;
    private final ProfileStatisticsService profileStatisticsService;

    @Transactional
    public ProfileFriendRequest sendRequest(UUID receiverUserId) {
        User current = authenticatedUser();
        if (current.getId().equals(receiverUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
        }

        User receiver = userRepository.findById(receiverUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (profileFriendRepository.existsByUserIdAndFriendId(current.getId(), receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists");
        }

        if (friendRequestRepository.existsByRequesterIdAndReceiverId(current.getId(), receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request already sent");
        }

        if (friendRequestRepository.existsByRequesterIdAndReceiverId(receiver.getId(), current.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This user has already sent you a friend request; accept it instead");
        }

        return friendRequestRepository.save(ProfileFriendRequest.builder()
                .requester(current)
                .receiver(receiver)
                .build());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void acceptRequest(UUID requestId) {
        User current = authenticatedUser();
        ProfileFriendRequest request = friendRequestRepository.findByIdAndReceiverId(requestId, current.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));

        User requester = request.getRequester();

        friendRequestRepository.delete(request);

        if (!profileFriendRepository.existsByUserIdAndFriendId(current.getId(), requester.getId())) {
            profileFriendRepository.save(ProfileFriend.builder().user(current).friend(requester).build());
            profileFriendRepository.save(ProfileFriend.builder().user(requester).friend(current).build());
        }

        profileStatisticsService.refreshSnapshot(current.getId());
        profileStatisticsService.refreshSnapshot(requester.getId());
    }

    @Transactional
    public void declineOrCancelRequest(UUID requestId) {
        User current = authenticatedUser();

        ProfileFriendRequest request = friendRequestRepository.findByIdAndReceiverId(requestId, current.getId())
                .or(() -> friendRequestRepository.findByIdAndRequesterId(requestId, current.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));

        friendRequestRepository.delete(request);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDTO> listReceivedRequests() {
        User current = authenticatedUser();
        return friendRequestRepository.findByReceiverIdOrderByCreatedAtDesc(current.getId()).stream()
                .map(request -> toDto(request, request.getRequester()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDTO> listSentRequests() {
        User current = authenticatedUser();
        return friendRequestRepository.findByRequesterIdOrderByCreatedAtDesc(current.getId()).stream()
                .map(request -> toDto(request, request.getReceiver()))
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void removeFriend(UUID friendUserId) {
        User current = authenticatedUser();
        User friend = userRepository.findById(friendUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!profileFriendRepository.existsByUserIdAndFriendId(current.getId(), friend.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found");
        }

        profileFriendRepository.deleteByUserIdAndFriendIdOrUserIdAndFriendId(
                current.getId(), friend.getId(), friend.getId(), current.getId());

        profileStatisticsService.refreshSnapshot(current.getId());
        profileStatisticsService.refreshSnapshot(friend.getId());
    }

    private FriendRequestDTO toDto(ProfileFriendRequest request, User counterpart) {
        return new FriendRequestDTO(
                request.getId(),
                counterpart.getId(),
                counterpart.getName(),
                counterpart.getProfilePicture(),
                request.getCreatedAt() == null ? null : request.getCreatedAt().atOffset(ZoneOffset.UTC));
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
