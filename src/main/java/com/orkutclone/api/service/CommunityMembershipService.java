package com.orkutclone.api.service;

import com.orkutclone.api.dto.community.JoinRequestDTO;
import com.orkutclone.api.dto.community.JoinResultDTO;
import com.orkutclone.api.model.Community;
import com.orkutclone.api.model.CommunityMembership;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.CommunityType;
import com.orkutclone.api.model.enums.MembershipStatus;
import com.orkutclone.api.repository.CommunityMembershipRepository;
import com.orkutclone.api.repository.CommunityRepository;
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

/**
 * Joining, leaving and — for moderated communities — approving or rejecting join requests.
 */
@Service
@RequiredArgsConstructor
public class CommunityMembershipService {

    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository membershipRepository;
    private final ProfileStatisticsService profileStatisticsService;

    /**
     * Joins a public community outright; in a moderated one it files a request that stays
     * {@code PENDING} until the owner decides. The returned status tells the two apart.
     */
    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public JoinResultDTO join(UUID communityId) {
        User current = authenticatedUser();
        Community community = findCommunity(communityId);

        if (membershipRepository.existsByUserIdAndCommunityId(current.getId(), communityId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already a member of, or awaiting approval for, this community");
        }

        MembershipStatus status = community.getType() == CommunityType.MODERATED
                ? MembershipStatus.PENDING
                : MembershipStatus.APPROVED;

        membershipRepository.save(CommunityMembership.builder()
                .user(current)
                .community(community)
                .status(status)
                .build());

        if (status == MembershipStatus.APPROVED) {
            syncMembersCount(community);
            profileStatisticsService.refreshSnapshot(current.getId());
        }

        return new JoinResultDTO(status.name());
    }

    /** Also withdraws a pending request, since neither leaves anything behind. */
    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void leave(UUID communityId) {
        User current = authenticatedUser();
        CommunityMembership membership = membershipRepository
                .findByUserIdAndCommunityId(current.getId(), communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membership not found"));

        membershipRepository.delete(membership);

        if (membership.getStatus() == MembershipStatus.APPROVED) {
            syncMembersCount(findCommunity(communityId));
            profileStatisticsService.refreshSnapshot(current.getId());
        }
    }

    @Transactional(readOnly = true)
    public List<JoinRequestDTO> listJoinRequests(UUID communityId) {
        requireOwner(findCommunity(communityId));

        return membershipRepository.findPendingRequestsByCommunityId(communityId).stream()
                .map(projection -> new JoinRequestDTO(
                        projection.getUserId(),
                        projection.getName(),
                        projection.getAvatar(),
                        projection.getRequestedAt() == null ? null : projection.getRequestedAt().atOffset(ZoneOffset.UTC)))
                .toList();
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void approve(UUID communityId, UUID userId) {
        Community community = findCommunity(communityId);
        requireOwner(community);

        CommunityMembership request = findPendingRequest(communityId, userId);
        request.setStatus(MembershipStatus.APPROVED);
        membershipRepository.save(request);

        syncMembersCount(community);
        profileStatisticsService.refreshSnapshot(userId);
    }

    @Transactional
    public void reject(UUID communityId, UUID userId) {
        requireOwner(findCommunity(communityId));
        membershipRepository.delete(findPendingRequest(communityId, userId));
    }

    private CommunityMembership findPendingRequest(UUID communityId, UUID userId) {
        CommunityMembership membership = membershipRepository
                .findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Join request not found"));

        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This user is already a member");
        }
        return membership;
    }

    private Community findCommunity(UUID communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Community not found"));
    }

    private void requireOwner(Community community) {
        User current = authenticatedUser();
        if (community.getOwner() == null || !community.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the community owner can do this");
        }
    }

    /** Recomputes the denormalized counter from the approved rows, which stay the source of truth. */
    private void syncMembersCount(Community community) {
        long approved = membershipRepository.countByCommunityId(community.getId());
        community.setMembersCount((int) approved);
        communityRepository.save(community);
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
