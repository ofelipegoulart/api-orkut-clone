package com.orkutclone.api.service;

import com.orkutclone.api.dto.album.*;
import com.orkutclone.api.model.Album;
import com.orkutclone.api.model.Photo;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.AlbumPrivacy;
import com.orkutclone.api.repository.AlbumRepository;
import com.orkutclone.api.repository.PhotoRepository;
import com.orkutclone.api.repository.ProfileFriendRepository;
import com.orkutclone.api.repository.UserRepository;
import com.orkutclone.api.support.AlbumPhotoStorageService;
import com.orkutclone.api.support.UploadedImage;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlbumService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AlbumRepository albumRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final ProfileFriendRepository profileFriendRepository;
    private final ProfileStatisticsService profileStatisticsService;
    private final AlbumPhotoStorageService photoStorage;

    @Transactional
    public AlbumResponse create(CreateAlbumRequest request) {
        User owner = authenticatedUser();

        Album album = albumRepository.save(Album.builder()
                .owner(owner)
                .title(request.title().trim())
                .description(request.description())
                .privacy(request.privacy())
                .build());

        return toResponse(album, 0);
    }

    @Transactional(readOnly = true)
    public AlbumsPageDTO list(UUID userId, int page, int size) {
        User viewer = authenticatedUser();
        userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        PageRequest pageRequest = pageRequest(page, size);
        Page<Album> albums = canSeePrivateAlbums(viewer, userId)
                ? albumRepository.findByOwnerIdOrderByCreatedAtDesc(userId, pageRequest)
                : albumRepository.findByOwnerIdAndPrivacyOrderByCreatedAtDesc(userId, AlbumPrivacy.PUBLIC, pageRequest);

        List<AlbumResponse> results = albums.getContent().stream()
                .map(album -> toResponse(album, photoRepository.countByAlbumId(album.getId())))
                .toList();

        return new AlbumsPageDTO(page, size, albums.getTotalElements(), albums.getTotalPages(), results);
    }

    @Transactional(readOnly = true)
    public AlbumDetailDTO getDetail(UUID albumId) {
        User viewer = authenticatedUser();
        Album album = findAlbumOrThrow(albumId);

        if (!canSeePrivateAlbums(viewer, album.getOwner().getId()) && album.getPrivacy() == AlbumPrivacy.FRIENDS_ONLY) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found");
        }

        List<Photo> photos = photoRepository.findByAlbumIdOrderByOrderIndexAsc(albumId);
        List<PhotoResponse> photoResponses = photos.stream().map(this::toPhotoResponse).toList();

        return new AlbumDetailDTO(
                album.getId(),
                album.getOwner().getId(),
                album.getTitle(),
                album.getDescription(),
                album.getPrivacy(),
                album.getCoverPhoto() == null ? null : album.getCoverPhoto().getUrl(),
                photos.size(),
                toOffsetDateTime(album.getCreatedAt()),
                toOffsetDateTime(album.getUpdatedAt()),
                photoResponses);
    }

    @Transactional
    public AlbumResponse update(UUID albumId, UpdateAlbumRequest request) {
        Album album = findAlbumOrThrow(albumId);
        requireOwner(album);

        album.setTitle(request.title().trim());
        album.setDescription(request.description());
        album.setPrivacy(request.privacy());
        albumRepository.save(album);

        return toResponse(album, photoRepository.countByAlbumId(albumId));
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void delete(UUID albumId) {
        Album album = findAlbumOrThrow(albumId);
        requireOwner(album);
        UUID ownerId = album.getOwner().getId();

        album.setCoverPhoto(null);
        albumRepository.save(album);

        photoRepository.deleteByAlbumId(albumId);
        albumRepository.delete(album);
        photoStorage.deleteAlbumDirectory(albumId);

        profileStatisticsService.refreshSnapshot(ownerId);
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public PhotoResponse uploadPhoto(UUID albumId, MultipartFile file) {
        Album album = findAlbumOrThrow(albumId);
        requireOwner(album);

        UploadedImage image = UploadedImage.from(file);
        String url = photoStorage.store(albumId, image.data(), image.extension());

        int orderIndex = (int) photoRepository.countByAlbumId(albumId);
        Photo photo = photoRepository.save(Photo.builder()
                .album(album)
                .url(url)
                .orderIndex(orderIndex)
                .build());

        if (album.getCoverPhoto() == null) {
            album.setCoverPhoto(photo);
            albumRepository.save(album);
        }

        profileStatisticsService.refreshSnapshot(album.getOwner().getId());
        return toPhotoResponse(photo);
    }

    @Transactional
    public PhotoResponse updateCaption(UUID albumId, UUID photoId, UpdatePhotoCaptionRequest request) {
        Album album = findAlbumOrThrow(albumId);
        requireOwner(album);
        Photo photo = findPhotoOrThrow(albumId, photoId);

        photo.setCaption(request.caption());
        photoRepository.save(photo);
        return toPhotoResponse(photo);
    }

    @Transactional
    public AlbumResponse setCover(UUID albumId, SetCoverRequest request) {
        Album album = findAlbumOrThrow(albumId);
        requireOwner(album);
        Photo photo = findPhotoOrThrow(albumId, request.photoId());

        album.setCoverPhoto(photo);
        albumRepository.save(album);

        return toResponse(album, photoRepository.countByAlbumId(albumId));
    }

    @Transactional
    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public void deletePhoto(UUID albumId, UUID photoId) {
        Album album = findAlbumOrThrow(albumId);
        requireOwner(album);
        Photo photo = findPhotoOrThrow(albumId, photoId);

        if (album.getCoverPhoto() != null && album.getCoverPhoto().getId().equals(photoId)) {
            album.setCoverPhoto(null);
            albumRepository.save(album);
        }

        photoRepository.delete(photo);
        photoStorage.delete(photo.getUrl());

        profileStatisticsService.refreshSnapshot(album.getOwner().getId());
    }

    private Album findAlbumOrThrow(UUID albumId) {
        return albumRepository.findById(albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found"));
    }

    private Photo findPhotoOrThrow(UUID albumId, UUID photoId) {
        return photoRepository.findByIdAndAlbumId(photoId, albumId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
    }

    private void requireOwner(Album album) {
        User current = authenticatedUser();
        if (!album.getOwner().getId().equals(current.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the album owner");
        }
    }

    /** The owner always sees their own albums; anyone else needs an accepted friendship. */
    private boolean canSeePrivateAlbums(User viewer, UUID ownerId) {
        return viewer.getId().equals(ownerId) || profileFriendRepository.existsByUserIdAndFriendId(viewer.getId(), ownerId);
    }

    private AlbumResponse toResponse(Album album, long photoCount) {
        return new AlbumResponse(
                album.getId(),
                album.getOwner().getId(),
                album.getTitle(),
                album.getDescription(),
                album.getPrivacy(),
                album.getCoverPhoto() == null ? null : album.getCoverPhoto().getUrl(),
                photoCount,
                toOffsetDateTime(album.getCreatedAt()),
                toOffsetDateTime(album.getUpdatedAt()));
    }

    private PhotoResponse toPhotoResponse(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getAlbum().getId(),
                photo.getUrl(),
                photo.getCaption(),
                photo.getOrderIndex(),
                toOffsetDateTime(photo.getCreatedAt()));
    }

    private static java.time.OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
