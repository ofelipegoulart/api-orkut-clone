package com.orkutclone.api.service;

import com.orkutclone.api.model.ProfileFriend;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.ProfileFriendRepository;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileFriendService {

    private final UserRepository userRepository;
    private final ProfileFriendRepository profileFriendRepository;
    private final ProfileStatisticsService profileStatisticsService;

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void addFriend(UUID friendUserId) {
        User current = authenticatedUser();
        if (current.getId().equals(friendUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot befriend yourself");
        }

        User friend = userRepository.findById(friendUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (profileFriendRepository.existsByUserIdAndFriendId(current.getId(), friend.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friendship already exists");
        }

        profileFriendRepository.save(ProfileFriend.builder().user(current).friend(friend).build());
        profileFriendRepository.save(ProfileFriend.builder().user(friend).friend(current).build());

        profileStatisticsService.refreshSnapshot(current.getId());
        profileStatisticsService.refreshSnapshot(friend.getId());
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

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}