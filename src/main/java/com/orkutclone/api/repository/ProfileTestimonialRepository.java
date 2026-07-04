package com.orkutclone.api.repository;

import com.orkutclone.api.model.ProfileTestimonial;
import com.orkutclone.api.model.enums.TestimonialStatus;
import com.orkutclone.api.repository.projection.TestimonialOverviewProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProfileTestimonialRepository extends JpaRepository<ProfileTestimonial, UUID> {

    @Query("SELECT COUNT(t) FROM ProfileTestimonial t WHERE t.target.id = :targetId AND t.status = com.orkutclone.api.model.enums.TestimonialStatus.APPROVED")
    long countByTargetId(@Param("targetId") UUID targetId);

    long countByTargetIdAndStatus(UUID targetId, TestimonialStatus status);

    java.util.List<ProfileTestimonial> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    java.util.List<ProfileTestimonial> findByTargetIdOrderByCreatedAtDesc(UUID targetId);

    java.util.List<ProfileTestimonial> findByTargetIdAndStatusOrderByCreatedAtDesc(UUID targetId, TestimonialStatus status);

    java.util.Optional<ProfileTestimonial> findByIdAndTargetId(UUID id, UUID targetId);

    @Query("""
            SELECT t.id as id,
                   t.author.id as authorId,
                   t.author.name as authorName,
                   t.author.profilePicture as authorAvatar,
                   t.message as message,
                   t.createdAt as createdAt
            FROM ProfileTestimonial t
            WHERE t.target.id = :targetId AND t.status = com.orkutclone.api.model.enums.TestimonialStatus.APPROVED
            ORDER BY t.createdAt DESC
            """)
    List<TestimonialOverviewProjection> findOverviewByTargetId(@Param("targetId") UUID targetId, Pageable pageable);
}