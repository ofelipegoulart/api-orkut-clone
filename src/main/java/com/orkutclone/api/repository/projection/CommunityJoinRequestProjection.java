package com.orkutclone.api.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface CommunityJoinRequestProjection {

    UUID getUserId();

    String getName();

    String getAvatar();

    Instant getRequestedAt();
}
