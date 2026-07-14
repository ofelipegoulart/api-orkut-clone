package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.CreateTestimonialRequest;
import com.orkutclone.api.dto.profile.ProfileOverviewDTO;
import com.orkutclone.api.model.ProfileTestimonial;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.TestimonialStatus;
import com.orkutclone.api.repository.ProfileTestimonialRepository;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
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
public class ProfileTestimonialService {

    private static final int MAX_LENGTH = 1024;

    private final UserRepository userRepository;
    private final ProfileTestimonialRepository testimonialRepository;
    private final ProfileStatisticsService profileStatisticsService;

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public ProfileTestimonial send(UUID targetUserId, CreateTestimonialRequest request) {
        User author = authenticatedUser();
        if (author.getId().equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a testimonial to yourself");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String message = sanitizeMessage(request.message());

        ProfileTestimonial testimonial = testimonialRepository.save(ProfileTestimonial.builder()
                .author(author)
                .target(target)
                .message(message)
                .status(TestimonialStatus.PENDING)
                .build());

        return testimonial;
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public ProfileTestimonial approve(UUID testimonialId) {
        User current = authenticatedUser();
        ProfileTestimonial testimonial = testimonialRepository.findByIdAndTargetId(testimonialId, current.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));

        testimonial.setStatus(TestimonialStatus.APPROVED);
        ProfileTestimonial saved = testimonialRepository.save(testimonial);
        profileStatisticsService.refreshSnapshot(current.getId());
        return saved;
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void reject(UUID testimonialId) {
        User current = authenticatedUser();
        ProfileTestimonial testimonial = testimonialRepository.findByIdAndTargetId(testimonialId, current.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));

        testimonialRepository.delete(testimonial);
        profileStatisticsService.refreshSnapshot(current.getId());
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void delete(UUID testimonialId) {
        User current = authenticatedUser();
        ProfileTestimonial testimonial = testimonialRepository.findById(testimonialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Testimonial not found"));

        boolean isAuthor = testimonial.getAuthor().getId().equals(current.getId());
        boolean isTarget = testimonial.getTarget().getId().equals(current.getId());

        if (!isAuthor && !isTarget) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or target can delete this testimonial");
        }

        UUID targetId = testimonial.getTarget().getId();
        testimonialRepository.delete(testimonial);
        profileStatisticsService.refreshSnapshot(targetId);
    }

    @Transactional(readOnly = true)
    public List<ProfileOverviewDTO.TestimonialDTO> findReceived(UUID targetUserId, boolean includePending) {
        List<ProfileTestimonial> testimonials = includePending
                ? testimonialRepository.findByTargetIdOrderByCreatedAtDesc(targetUserId)
                : testimonialRepository.findByTargetIdAndStatusOrderByCreatedAtDesc(targetUserId, TestimonialStatus.APPROVED);
        return toDtos(testimonials);
    }

    @Transactional(readOnly = true)
    public List<ProfileOverviewDTO.TestimonialDTO> findSent(UUID authorUserId) {
        return toDtos(testimonialRepository.findByAuthorIdOrderByCreatedAtDesc(authorUserId));
    }

    private List<ProfileOverviewDTO.TestimonialDTO> toDtos(List<ProfileTestimonial> testimonials) {
        return testimonials.stream()
                .map(testimonial -> new ProfileOverviewDTO.TestimonialDTO(
                        testimonial.getId(),
                        testimonial.getAuthor().getId(),
                        testimonial.getAuthor().getName(),
                        testimonial.getAuthor().getProfilePicture(),
                        testimonial.getMessage(),
                        testimonial.getStatus().name(),
                        testimonial.getCreatedAt() == null ? null : testimonial.getCreatedAt().atOffset(ZoneOffset.UTC)))
                .toList();
    }

    private String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Testimonial message cannot be empty");
        }
        if (message.length() > MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Testimonial message exceeds maximum length of 1024 characters");
        }

        String stripped = Jsoup.parseBodyFragment(message).text();
        if (!stripped.equals(message)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Testimonial cannot contain HTML");
        }
        return message;
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}