package com.orkutclone.api.repository.projection;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TestimonialOverviewProjection {
    UUID getId();
    UUID getAuthorId();
    String getAuthorName();
    String getAuthorAvatar();
    String getMessage();
    LocalDateTime getCreatedAt();
}