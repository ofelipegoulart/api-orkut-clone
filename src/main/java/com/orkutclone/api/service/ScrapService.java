package com.orkutclone.api.service;

import com.orkutclone.api.dto.CreateScrapRequest;
import com.orkutclone.api.dto.ScrapResponse;
import com.orkutclone.api.dto.UpdateScrapRequest;
import com.orkutclone.api.model.Scrap;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.ScrapRepository;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScrapService {

    private final ScrapRepository scrapRepository;
    private final UserRepository userRepository;

    public ScrapResponse create(CreateScrapRequest request) {
        User author = authenticatedUser();
        User owner = userRepository.findById(request.ownerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Scrap scrap = Scrap.builder()
                .content(request.content())
                .author(author)
                .owner(owner)
                .isPrivate(Boolean.TRUE.equals(request.isPrivate()))
                .build();

        return toResponse(scrapRepository.save(scrap));
    }

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

    public ScrapResponse update(UUID scrapId, UpdateScrapRequest request) {
        User current = authenticatedUser();
        Scrap scrap = findScrapOrThrow(scrapId);

        if (!scrap.getAuthor().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can edit this scrap");
        }

        scrap.setContent(request.content());
        return toResponse(scrapRepository.save(scrap));
    }

    public void delete(UUID scrapId) {
        User current = authenticatedUser();
        Scrap scrap = findScrapOrThrow(scrapId);

        boolean isAuthor = scrap.getAuthor().getId().equals(current.getId());
        boolean isWallOwner = scrap.getOwner().getId().equals(current.getId());

        if (!isAuthor && !isWallOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or wall owner can delete this scrap");
        }

        scrapRepository.delete(scrap);
    }

    public Page<ScrapResponse> findByOwner(UUID ownerId, int page, int size) {
        if (!userRepository.existsById(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        UUID viewerId = authenticatedUser().getId();
        return scrapRepository.findVisibleByOwnerId(ownerId, viewerId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public Page<ScrapResponse> findSent(int page, int size) {
        UUID authorId = authenticatedUser().getId();
        return scrapRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    private Scrap findScrapOrThrow(UUID scrapId) {
        return scrapRepository.findById(scrapId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scrap not found"));
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private ScrapResponse toResponse(Scrap scrap) {
        return new ScrapResponse(
                scrap.getId(),
                scrap.getContent(),
                scrap.isPrivate(),
                scrap.getAuthor().getId(),
                scrap.getAuthor().getName(),
                scrap.getOwner().getId(),
                scrap.getOwner().getName(),
                scrap.getCreatedAt(),
                scrap.getUpdatedAt()
        );
    }
}
