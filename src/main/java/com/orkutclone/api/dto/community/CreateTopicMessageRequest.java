package com.orkutclone.api.dto.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload to post a message to a topic. The topic (and therefore its community) is
 * identified by the request path; only the subject and body travel in the body.
 */
public record CreateTopicMessageRequest(
        @NotBlank @Size(max = 255) String subject,

        @NotBlank
        @Size(max = 2048, message = "Message must be at most 2048 characters")
        @Pattern(regexp = "[^<>]*", message = "Message must not contain HTML")
        String message
) {}
