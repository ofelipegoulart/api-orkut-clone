package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.CreateProfileRatingRequest;
import com.orkutclone.api.dto.profile.ProfileOverviewDTO;
import com.orkutclone.api.model.ProfileRating;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.ProfileRatingRepository;
import com.orkutclone.api.repository.UserRepository;
import com.orkutclone.api.repository.projection.RatingSummaryProjection;
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
public class ProfileRatingService {

    private final UserRepository userRepository;
    private final ProfileRatingRepository profileRatingRepository;

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public ProfileRating rate(UUID targetUserId, CreateProfileRatingRequest request) {
        User current = authenticatedUser();
        if (current.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot rate your own profile");
        }

        if (request.legal() == null && request.trustworthy() == null && request.sexy() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one category must be rated");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ProfileRating rating = profileRatingRepository.findByTargetIdAndRaterId(targetUserId, current.getId())
                .orElseGet(ProfileRating::new);
        rating.setTarget(target);
        rating.setRater(current);

        if (request.legal() != null) {
            rating.setLegalPercentage(toFraction(request.legal(), "legal"));
        }
        if (request.trustworthy() != null) {
            rating.setTrustworthyPercentage(toFraction(request.trustworthy(), "trustworthy"));
        }
        if (request.sexy() != null) {
            rating.setSexyPercentage(toFraction(request.sexy(), "sexy"));
        }

        return profileRatingRepository.save(rating);
    }

    @Transactional(readOnly = true)
    public ProfileOverviewDTO.RatingsDTO getAverageRatings(UUID targetUserId) {
        if (!userRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        RatingSummaryProjection summary = profileRatingRepository.findSummaryByTargetId(targetUserId);
        return new ProfileOverviewDTO.RatingsDTO(
                summary.getLegalPercentage() == null ? 0D : summary.getLegalPercentage(),
                summary.getTrustworthyPercentage() == null ? 0D : summary.getTrustworthyPercentage(),
                summary.getSexyPercentage() == null ? 0D : summary.getSexyPercentage());
    }

    @Transactional(readOnly = true)
    public ProfileOverviewDTO.RatingsDTO getPublicRatings(UUID targetUserId) {
        ProfileRating rating = profileRatingRepository.findByTargetIdAndRaterId(targetUserId, authenticatedUser().getId())
                .orElse(null);

        return new ProfileOverviewDTO.RatingsDTO(
                rating != null && rating.getLegalPercentage() != null ? rating.getLegalPercentage() : 0D,
                rating != null && rating.getTrustworthyPercentage() != null ? rating.getTrustworthyPercentage() : 0D,
                rating != null && rating.getSexyPercentage() != null ? rating.getSexyPercentage() : 0D);
    }

    private double toFraction(Integer step, String fieldName) {
        if (step < 1 || step > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be between 1 and 6");
        }
        return step / 6.0;
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}