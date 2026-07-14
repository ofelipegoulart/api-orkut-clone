package com.orkutclone.api.model.enums;

public enum MembershipStatus {
    /** Join request awaiting the owner's approval; the user is not a member yet. */
    PENDING,
    /** Effective membership. Only these count towards members lists and counters. */
    APPROVED
}
