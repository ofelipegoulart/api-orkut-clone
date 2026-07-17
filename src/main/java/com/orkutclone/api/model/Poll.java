package com.orkutclone.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "polls", indexes = {
        @Index(name = "idx_polls_community", columnList = "community_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(nullable = false, length = 280)
    private String question;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** When the poll stops accepting votes; null means it never closes on its own. */
    @Column(name = "closes_at")
    private Instant closesAt;

    /**
     * When true, a voter may check more than one option in their single vote; when false
     * (the default) they must pick exactly one. Either way, a voter only gets to vote once.
     */
    @Column(name = "multiple_choice", nullable = false)
    @Builder.Default
    private boolean multipleChoice = false;

    /**
     * When true, individual votes are not exposed anywhere in the API — e.g. comments never
     * carry a "voted for X" tag for this poll. Aggregate counts per option are always public
     * either way.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean anonymous = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
