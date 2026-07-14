package com.orkutclone.api.model;

import com.orkutclone.api.model.enums.CommunityCategory;
import com.orkutclone.api.model.enums.CommunityContentPrivacy;
import com.orkutclone.api.model.enums.CommunityType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private String icon;

    /** Kept as a free-form string (e.g. "Portuguese"); promote to an enum later if the values need to be constrained. */
    private String language;

    @Enumerated(EnumType.STRING)
    private CommunityCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommunityType type = CommunityType.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_privacy", nullable = false)
    @Builder.Default
    private CommunityContentPrivacy contentPrivacy = CommunityContentPrivacy.OPEN_TO_NON_MEMBERS;

    @Embedded
    private CommunityLocation location;

    @Embedded
    @Builder.Default
    private CommunityFeatureSettings features = CommunityFeatureSettings.builder().build();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    /**
     * Denormalized member counter for fast reads on the dashboard/grid.
     * Kept in sync by the membership flow; treat {@code CommunityMembership} as the source of truth.
     */
    @Column(name = "members_count", nullable = false)
    @Builder.Default
    private Integer membersCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}