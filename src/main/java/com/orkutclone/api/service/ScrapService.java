package com.orkutclone.api.service;

import com.orkutclone.api.dto.CreateScrapRequest;
import com.orkutclone.api.dto.ScrapResponse;
import com.orkutclone.api.dto.UpdateScrapRequest;
import com.orkutclone.api.model.Scrap;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.ScrapRepository;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScrapService {

    private static final int MAX_CONTENT_LENGTH = 1024;
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 20, 50);
    private static final Safelist ALLOWED_HTML = Safelist.basicWithImages()
            .addTags("span", "div", "h1", "h2", "h3")
            .addAttributes(":all", "style", "class");

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final ProfileStatisticsService profileStatisticsService;

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public ScrapResponse create(CreateScrapRequest request) {
        User author = authenticatedUser();
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (author.getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a scrap to yourself");
        }

        String sanitized = sanitizeContent(request.content());

        Scrap parent = null;
        if (request.parentId() != null) {
            parent = findScrapOrThrow(request.parentId());
        }

        Scrap scrap = Scrap.builder()
                .content(sanitized)
                .author(author)
                .owner(owner)
                .parent(parent)
                .isPrivate(Boolean.TRUE.equals(request.isPrivate()))
                .build();

        ScrapResponse response = toResponse(scrapRepository.save(scrap));
        profileStatisticsService.refreshSnapshot(owner.getId());
        return response;
    }

    @Transactional(readOnly = true)
    public ScrapResponse findById(UUID scrapId) {
        User current = authenticatedUser();
        Scrap scrap = findScrapOrThrow(scrapId);

        if (scrap.isPrivate()
                && !scrap.getAuthor().getId().equals(current.getId())
                && !scrap.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This scrap is private");
        }

        return toResponse(scrap);
    }

    @Transactional
    public ScrapResponse update(UUID scrapId, UpdateScrapRequest request) {
        User current = authenticatedUser();
        Scrap scrap = findScrapOrThrow(scrapId);

        if (!scrap.getAuthor().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can edit this scrap");
        }

        scrap.setContent(sanitizeContent(request.content()));
        return toResponse(scrapRepository.save(scrap));
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void delete(UUID scrapId) {
        User current = authenticatedUser();
        Scrap scrap = findScrapOrThrow(scrapId);

        boolean isAuthor = scrap.getAuthor().getId().equals(current.getId());
        boolean isWallOwner = scrap.getOwner().getId().equals(current.getId());

        if (!isAuthor && !isWallOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or wall owner can delete this scrap");
        }

        UUID ownerId = scrap.getOwner().getId();
        scrapRepository.delete(scrap);
        profileStatisticsService.refreshSnapshot(ownerId);
    }

    @Transactional(readOnly = true)
    public Page<ScrapResponse> findByOwner(UUID ownerId, int page, int size) {
        validatePageSize(size);
        if (!userRepository.existsById(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        UUID viewerId = authenticatedUser().getId();
        return scrapRepository.findVisibleByOwnerId(ownerId, viewerId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ScrapResponse> findSent(int page, int size) {
        validatePageSize(size);
        UUID authorId = authenticatedUser().getId();
        return scrapRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void deleteMultiple(List<UUID> scrapIds) {
        if (scrapIds == null || scrapIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No scrap IDs provided");
        }
        User current = authenticatedUser();
        java.util.Set<UUID> ownerIds = new java.util.HashSet<>();
        for (UUID scrapId : scrapIds) {
            Scrap scrap = findScrapOrThrow(scrapId);
            boolean isAuthor = scrap.getAuthor().getId().equals(current.getId());
            boolean isWallOwner = scrap.getOwner().getId().equals(current.getId());
            if (!isAuthor && !isWallOwner) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the author or wall owner can delete scrap " + scrapId);
            }
            ownerIds.add(scrap.getOwner().getId());
        }
        for (UUID scrapId : scrapIds) {
            scrapRepository.deleteById(scrapId);
        }
        ownerIds.forEach(profileStatisticsService::refreshSnapshot);
    }

    @Transactional(readOnly = true)
    public List<ScrapResponse> findThread(UUID scrapId) {
        Scrap scrap = findScrapOrThrow(scrapId);
        User current = authenticatedUser();

        Scrap root = scrap;
        while (root.getParent() != null) {
            root = root.getParent();
        }

        if (root.isPrivate()
                && !root.getAuthor().getId().equals(current.getId())
                && !root.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This conversation is private");
        }

        List<ScrapResponse> thread = new java.util.ArrayList<>();
        thread.add(toResponse(root));
        addReplies(root.getId(), current, thread);
        return thread;
    }

    private void addReplies(UUID parentId, User viewer, List<ScrapResponse> thread) {
        List<Scrap> replies = scrapRepository.findByParentIdOrderByCreatedAtAsc(parentId);
        for (Scrap reply : replies) {
            if (!reply.isPrivate()
                    || reply.getAuthor().getId().equals(viewer.getId())
                    || reply.getOwner().getId().equals(viewer.getId())) {
                thread.add(toResponse(reply));
            }
            addReplies(reply.getId(), viewer, thread);
        }
    }

    @Transactional
    public int markAsRead(List<UUID> scrapIds) {
        if (scrapIds == null || scrapIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No scrap IDs provided");
        }
        UUID ownerId = authenticatedUser().getId();
        return scrapRepository.markAsRead(scrapIds, ownerId);
    }

    public long countUnread(UUID ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return scrapRepository.countUnreadByOwnerId(ownerId);
    }

    private void validatePageSize(int size) {
        if (!ALLOWED_PAGE_SIZES.contains(size)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be one of: " + ALLOWED_PAGE_SIZES);
        }
    }

    private String sanitizeContent(String content) {
        String clean = Jsoup.clean(content, ALLOWED_HTML);
        if (clean.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Scrap content cannot be empty");
        }
        if (clean.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Scrap content exceeds maximum length of " + MAX_CONTENT_LENGTH + " characters");
        }
        return clean;
    }

    private Scrap findScrapOrThrow(UUID scrapId) {
        return scrapRepository.findById(scrapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scrap not found"));
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private ScrapResponse toResponse(Scrap scrap) {
        User author = scrap.getAuthor();
        return new ScrapResponse(
                scrap.getId(),
                scrap.getContent(),
                scrap.isPrivate(),
                author.getId(),
                author.getName(),
                author.getProfilePicture(),
                scrap.getOwner().getId(),
                scrap.getOwner().getName(),
                scrap.getParent() != null ? scrap.getParent().getId() : null,
                scrap.getReadAt(),
                scrap.getCreatedAt() != null ? scrap.getCreatedAt().atOffset(ZoneOffset.UTC) : null,
                scrap.getUpdatedAt() != null ? scrap.getUpdatedAt().atOffset(ZoneOffset.UTC) : null
        );
    }

}
