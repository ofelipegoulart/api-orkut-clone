package com.orkutclone.api.model;

import com.orkutclone.api.model.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "community_memberships", uniqueConstraints = {
        @UniqueConstraint(name = "uk_community_memberships_user_community", columnNames = {"user_id", "community_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /**
     * A row only represents an effective membership when {@code APPROVED}. Moderated
     * communities park join requests as {@code PENDING} until the owner acts on them.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.APPROVED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}