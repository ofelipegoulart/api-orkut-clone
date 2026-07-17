package com.orkutclone.api.repository.projection;

import java.util.UUID;

public interface PollOptionVoteCountProjection {
    UUID getOptionId();
    Long getVoteCount();
}
