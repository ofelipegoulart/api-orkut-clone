package com.orkutclone.api.controller;

import com.orkutclone.api.dto.poll.*;
import com.orkutclone.api.service.PollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/community/{communityId}/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a poll image", description = "Returns the public URL to send as `imageUrl` when creating the poll.")
    @ApiResponse(responseCode = "200", description = "Image uploaded")
    @ApiResponse(responseCode = "400", description = "Missing, oversized, or unsupported image")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<PollImageResponse> uploadImage(
            @PathVariable UUID communityId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(pollService.uploadImage(file));
    }

    @PostMapping
    @Operation(summary = "Create a poll in a community")
    @ApiResponse(responseCode = "201", description = "Poll created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Polls are disabled for this community")
    @ApiResponse(responseCode = "404", description = "Community not found")
    public ResponseEntity<PollDetailDTO> create(
            @PathVariable UUID communityId,
            @RequestBody @Valid CreatePollRequest request) {
        return ResponseEntity.status(201).body(pollService.create(communityId, request));
    }

    @GetMapping
    @Operation(summary = "List a community's polls, most recent first")
    @ApiResponse(responseCode = "200", description = "Polls retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Community not found")
    public ResponseEntity<PollsPageDTO> list(
            @PathVariable UUID communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(pollService.list(communityId, page, size));
    }

    @GetMapping("/{pollId}")
    @Operation(summary = "Get a poll's detail, including options, vote counts and comments")
    @ApiResponse(responseCode = "200", description = "Poll retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Community or poll not found")
    public ResponseEntity<PollDetailDTO> getDetail(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId) {
        return ResponseEntity.ok(pollService.getDetail(communityId, pollId));
    }

    @DeleteMapping("/{pollId}")
    @Operation(summary = "Delete a poll (community owner only)")
    @ApiResponse(responseCode = "204", description = "Poll deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the community owner")
    @ApiResponse(responseCode = "404", description = "Community or poll not found")
    public ResponseEntity<Void> delete(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId) {
        pollService.delete(communityId, pollId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{pollId}/vote")
    @Operation(summary = "Vote in a poll",
            description = "One vote per user per poll; checks one option, or several if the poll is `multipleChoice`.")
    @ApiResponse(responseCode = "200", description = "Vote recorded")
    @ApiResponse(responseCode = "400", description = "Validation failed, or more than one option sent to a single-choice poll")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Polls are disabled for this community")
    @ApiResponse(responseCode = "404", description = "Community, poll, or option not found")
    @ApiResponse(responseCode = "409", description = "Already voted in this poll, or the poll is closed")
    public ResponseEntity<PollDetailDTO> vote(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId,
            @RequestBody @Valid VoteRequest request) {
        return ResponseEntity.ok(pollService.vote(communityId, pollId, request));
    }

    @PostMapping("/{pollId}/comments")
    @Operation(summary = "Comment on a poll")
    @ApiResponse(responseCode = "201", description = "Comment posted")
    @ApiResponse(responseCode = "400", description = "Validation failed (blank, over 500 chars, or contains HTML)")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Polls are disabled for this community")
    @ApiResponse(responseCode = "404", description = "Community or poll not found")
    public ResponseEntity<PollCommentDTO> addComment(
            @PathVariable UUID communityId,
            @PathVariable UUID pollId,
            @RequestBody @Valid CreatePollCommentRequest request) {
        return ResponseEntity.status(201).body(pollService.addComment(communityId, pollId, request));
    }
}
