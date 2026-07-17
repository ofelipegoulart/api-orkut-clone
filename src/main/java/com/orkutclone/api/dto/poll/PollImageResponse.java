package com.orkutclone.api.dto.poll;

/** Public URL of the uploaded image, to be sent back as {@code imageUrl} when creating the poll. */
public record PollImageResponse(String url) {}
