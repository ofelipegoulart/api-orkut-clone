package com.orkutclone.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Per-community toggles for the optional features (forum, polls, events, custom news),
 * mirroring the "Configurações de recursos da comunidade" section of the creation form.
 * Defaults here are the ones the form pre-selects.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityFeatureSettings {

    @Column(name = "forum_enabled", nullable = false)
    @Builder.Default
    private boolean forumEnabled = true;

    @Column(name = "forum_on_homepage", nullable = false)
    @Builder.Default
    private boolean forumOnHomepage = true;

    /**
     * Stored for the frontend; every API write is authenticated, so there are no
     * anonymous posts to reject server-side today.
     */
    @Column(name = "forum_no_anonymous_posts", nullable = false)
    @Builder.Default
    private boolean forumNoAnonymousPosts = true;

    @Column(name = "polls_enabled", nullable = false)
    @Builder.Default
    private boolean pollsEnabled = true;

    @Column(name = "polls_on_homepage", nullable = false)
    @Builder.Default
    private boolean pollsOnHomepage = true;

    @Column(name = "events_enabled", nullable = false)
    @Builder.Default
    private boolean eventsEnabled = true;

    @Column(name = "events_on_homepage", nullable = false)
    @Builder.Default
    private boolean eventsOnHomepage = false;

    @Column(name = "custom_news_enabled", nullable = false)
    @Builder.Default
    private boolean customNewsEnabled = false;
}
