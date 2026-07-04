package com.orkutclone.api.repository.projection;

import java.util.UUID;

public interface CommunityOverviewProjection {
    UUID getId();
    String getName();
    String getIcon();
}