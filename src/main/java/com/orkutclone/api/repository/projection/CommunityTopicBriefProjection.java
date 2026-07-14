package com.orkutclone.api.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface CommunityTopicBriefProjection {
    UUID getId();
    String getTitle();
    long getTotalPosts();
    Instant getLastPostDate();
}
