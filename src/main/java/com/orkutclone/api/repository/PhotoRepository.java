package com.orkutclone.api.repository;

import com.orkutclone.api.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findByAlbumIdOrderByOrderIndexAsc(UUID albumId);

    Optional<Photo> findByIdAndAlbumId(UUID id, UUID albumId);

    long countByAlbumId(UUID albumId);

    long countByAlbum_Owner_Id(UUID ownerId);

    void deleteByAlbumId(UUID albumId);
}
