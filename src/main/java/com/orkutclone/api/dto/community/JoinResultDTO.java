package com.orkutclone.api.dto.community;

/**
 * Outcome of a join attempt: {@code APPROVED} in a public community (the user is already
 * a member), {@code PENDING} in a moderated one (the owner still has to approve).
 */
public record JoinResultDTO(String status) {}
