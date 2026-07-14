package com.orkutclone.api.repository.projection;

import java.util.UUID;

public interface CommunityMemberProjection {
    UUID getId();
    String getName();
    String getAvatar();
    Long getFriendsCount();
}
