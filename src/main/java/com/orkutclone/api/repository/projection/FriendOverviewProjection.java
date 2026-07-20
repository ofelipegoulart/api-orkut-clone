package com.orkutclone.api.repository.projection;

import java.util.UUID;

public interface FriendOverviewProjection {
    UUID getId();
    String getName();
    String getAvatar();
    Long getFriendsCount();
    String getGender();
    String getCity();
    String getRelationshipStatus();
}