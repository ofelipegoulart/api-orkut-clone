package com.orkutclone.api.repository;

import com.orkutclone.api.model.Album;
import com.orkutclone.api.model.enums.AlbumPrivacy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlbumRepository extends JpaRepository<Album, UUID> {

    Page<Album> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId, Pageable pageable);

    Page<Album> findByOwnerIdAndPrivacyOrderByCreatedAtDesc(UUID ownerId, AlbumPrivacy privacy, Pageable pageable);
}
