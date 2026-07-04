package com.orkutclone.api.repository;

import com.orkutclone.api.model.Scrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ScrapRepository extends JpaRepository<Scrap, UUID> {

    @Query("SELECT s FROM Scrap s WHERE s.owner.id = :ownerId AND (s.isPrivate = false OR s.author.id = :viewerId OR :viewerId = :ownerId) ORDER BY s.createdAt DESC")
    Page<Scrap> findVisibleByOwnerId(@Param("ownerId") UUID ownerId, @Param("viewerId") UUID viewerId, Pageable pageable);

    Page<Scrap> findByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    long countByOwnerId(UUID ownerId);

    List<Scrap> findByParentIdOrderByCreatedAtAsc(UUID parentId);

    @Modifying
    @Query(value = "UPDATE scraps SET read_at = NOW() WHERE id IN :ids AND read_at IS NULL AND owner_id = :ownerId", nativeQuery = true)
    int markAsRead(@Param("ids") List<UUID> ids, @Param("ownerId") UUID ownerId);

    @Query("SELECT COUNT(s) FROM Scrap s WHERE s.owner.id = :ownerId AND s.readAt IS NULL")
    long countUnreadByOwnerId(@Param("ownerId") UUID ownerId);
}
