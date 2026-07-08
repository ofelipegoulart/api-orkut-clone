package com.orkutclone.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    @Column(name = "scraps_count", nullable = false)
    private long scrapsCount = 0L;

    @Builder.Default
    @Column(name = "friends_count", nullable = false)
    private long friendsCount = 0L;

    @Builder.Default
    @Column(name = "communities_count", nullable = false)
    private long communitiesCount = 0L;

    @Builder.Default
    @Column(name = "testimonials_count", nullable = false)
    private long testimonialsCount = 0L;

    @Builder.Default
    @Column(name = "fans_count", nullable = false)
    private long fansCount = 0L;

    @Builder.Default
    @Column(name = "photos_count", nullable = false)
    private long photosCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}