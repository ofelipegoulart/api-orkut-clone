package com.orkutclone.api.service;

import com.orkutclone.api.model.ProfileStatistics;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileStatisticsService {

    private final UserRepository userRepository;
    private final ScrapRepository scrapRepository;
    private final ProfileFriendRepository profileFriendRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final ProfileTestimonialRepository profileTestimonialRepository;
    private final ProfileStatisticsRepository profileStatisticsRepository;

    @Transactional
    public ProfileStatistics getOrCreateSnapshot(UUID userId) {
        return profileStatisticsRepository.findByUserId(userId)
                .orElseGet(() -> rebuildSnapshot(userId));
    }

    @Transactional
    public ProfileStatistics refreshSnapshot(UUID userId) {
        return rebuildSnapshot(userId);
    }

    private ProfileStatistics rebuildSnapshot(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ProfileStatistics statistics = profileStatisticsRepository.findByUserId(userId)
                .orElseGet(ProfileStatistics::new);
        statistics.setUser(user);
        statistics.setScrapsCount(scrapRepository.countByOwnerId(userId));
        statistics.setFriendsCount(profileFriendRepository.countByUserId(userId));
        statistics.setCommunitiesCount(communityMembershipRepository.countByUserId(userId));
        statistics.setTestimonialsCount(profileTestimonialRepository.countByTargetId(userId));
        return profileStatisticsRepository.save(statistics);
    }
}