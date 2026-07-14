package com.orkutclone.api.dto.community;

/** One entry of the category dropdown: {@code value} is what the create endpoint expects, {@code label} is what the user sees. */
public record CategoryOptionDTO(String value, String label) {}
