package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.CreateCommunityRequest;
import com.orkutclone.api.dto.profile.ProfileOverviewDTO;
import com.orkutclone.api.model.Community;
import com.orkutclone.api.model.CommunityMembership;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.CommunityMembershipRepository;
import com.orkutclone.api.repository.CommunityRepository;
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
public class ProfileCommunityService {

    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final ProfileStatisticsService profileStatisticsService;

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public ProfileOverviewDTO.CommunityCardDTO create(CreateCommunityRequest request) {
        User current = authenticatedUser();
        Community community = communityRepository.save(Community.builder()
                .name(request.name().trim())
                .icon(request.icon())
                .owner(current)
                .build());

        communityMembershipRepository.save(CommunityMembership.builder()
                .user(current)
                .community(community)
                .build());

        profileStatisticsService.refreshSnapshot(current.getId());
        return new ProfileOverviewDTO.CommunityCardDTO(community.getId(), community.getName(), community.getIcon());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void join(UUID communityId) {
        User current = authenticatedUser();
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));

        if (communityMembershipRepository.existsByUserIdAndCommunityId(current.getId(), communityId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already a member of this community");
        }

        communityMembershipRepository.save(CommunityMembership.builder()
                .user(current)
                .community(community)
                .build());

        profileStatisticsService.refreshSnapshot(current.getId());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void leave(UUID communityId) {
        User current = authenticatedUser();
        if (!communityMembershipRepository.existsByUserIdAndCommunityId(current.getId(), communityId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found");
        }

        communityMembershipRepository.deleteByUserIdAndCommunityId(current.getId(), communityId);
        profileStatisticsService.refreshSnapshot(current.getId());
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}