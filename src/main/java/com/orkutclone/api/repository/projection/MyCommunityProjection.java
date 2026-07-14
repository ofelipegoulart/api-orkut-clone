package com.orkutclone.api.repository.projection;

import com.orkutclone.api.model.enums.MembershipStatus;

/**
 * A community the current user is tied to, plus how: they own it, are an approved member,
 * or are still awaiting approval.
 */
public interface MyCommunityProjection extends CommunityListItemProjection {

    /** True when the current user owns the community. */
    boolean getOwnedByUser();

    /** {@code APPROVED} or {@code PENDING}; null when the user only owns the community. */
    MembershipStatus getMembershipStatus();
}
