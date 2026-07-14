package com.orkutclone.api.repository.projection;

import java.time.Instant;
import java.util.UUID;

/**
 * A community as it appears in a list: identity plus the two aggregates the cards show,
 * the approved-member count and the date of the community's most recent forum post.
 */
public interface CommunityListItemProjection {

    UUID getId();

    String getName();

    String getIcon();

    long getMembersCount();

    Instant getLastPostDate();
}
