package com.orkutclone.api.controller;

import com.orkutclone.api.dto.community.*;
import com.orkutclone.api.model.enums.CommunityCategory;
import com.orkutclone.api.service.CommunityDashboardService;
import com.orkutclone.api.service.CommunityMembershipService;
import com.orkutclone.api.service.CommunityService;
import com.orkutclone.api.service.CommunityTopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final CommunityDashboardService communityDashboardService;
    private final CommunityMembershipService communityMembershipService;
    private final CommunityTopicService communityTopicService;

    @PostMapping
    @Operation(summary = "Create a community", description = "The creator becomes the owner and first member.")
    @ApiResponse(responseCode = "201", description = "Community created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<CommunityDetailDTO> create(@RequestBody @Valid CreateCommunityRequest request) {
        return ResponseEntity.status(201).body(communityService.create(request));
    }

    @GetMapping
    @Operation(summary = "List the communities of a category", description = "Most members first.")
    @ApiResponse(responseCode = "200", description = "Communities retrieved")
    @ApiResponse(responseCode = "400", description = "Unknown category")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<List<CommunityListItemDTO>> listByCategory(
            @RequestParam CommunityCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(communityService.listByCategory(category, page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "Search communities by name", description = "Case-insensitive, matches any part of the name.")
    @ApiResponse(responseCode = "200", description = "Search results (empty when the query is blank)")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<List<CommunityListItemDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(communityService.searchByName(name, page, size));
    }

    @GetMapping("/mine")
    @Operation(summary = "List the current user's communities",
            description = "Everything the user owns, belongs to, or is awaiting approval in; `relation` says which.")
    @ApiResponse(responseCode = "200", description = "Communities retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<List<MyCommunityDTO>> listMine() {
        return ResponseEntity.ok(communityService.listMine());
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Join a community",
            description = "Joins a public community outright. In a moderated one it files a request the owner must approve; the returned status says which happened.")
    @ApiResponse(responseCode = "200", description = "Joined (APPROVED) or join request filed (PENDING)")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Community not found")
    @ApiResponse(responseCode = "409", description = "Already a member or already awaiting approval")
    public ResponseEntity<JoinResultDTO> join(@PathVariable UUID id) {
        return ResponseEntity.ok(communityMembershipService.join(id));
    }

    @DeleteMapping("/{id}/leave")
    @Operation(summary = "Leave a community, or withdraw a pending join request")
    @ApiResponse(responseCode = "204", description = "Left community")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Membership not found")
    public ResponseEntity<Void> leave(@PathVariable UUID id) {
        communityMembershipService.leave(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    @Operation(summary = "List the categories offered by the creation form")
    @ApiResponse(responseCode = "200", description = "Categories retrieved")
    public ResponseEntity<List<CategoryOptionDTO>> listCategories() {
        return ResponseEntity.ok(communityService.listCategories());
    }

    @PostMapping(value = "/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a community image",
            description = "Returns the public URL to send as `icon` when creating the community.")
    @ApiResponse(responseCode = "200", description = "Image uploaded")
    @ApiResponse(responseCode = "400", description = "Missing, oversized, or unsupported image")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<CommunityIconResponse> uploadIcon(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(communityService.uploadIcon(file));
    }

    @GetMapping("/{id}/join-requests")
    @Operation(summary = "List pending join requests (owner only, moderated communities)")
    @ApiResponse(responseCode = "200", description = "Join requests retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the community owner")
    @ApiResponse(responseCode = "404", description = "Community not found")
    public ResponseEntity<List<JoinRequestDTO>> listJoinRequests(@PathVariable UUID id) {
        return ResponseEntity.ok(communityMembershipService.listJoinRequests(id));
    }

    @PostMapping("/{id}/join-requests/{userId}/approve")
    @Operation(summary = "Approve a pending join request (owner only)")
    @ApiResponse(responseCode = "204", description = "Request approved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the community owner")
    @ApiResponse(responseCode = "404", description = "Join request not found")
    @ApiResponse(responseCode = "409", description = "User is already a member")
    public ResponseEntity<Void> approveJoinRequest(@PathVariable UUID id, @PathVariable UUID userId) {
        communityMembershipService.approve(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/join-requests/{userId}")
    @Operation(summary = "Reject a pending join request (owner only)")
    @ApiResponse(responseCode = "204", description = "Request rejected")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the community owner")
    @ApiResponse(responseCode = "404", description = "Join request not found")
    @ApiResponse(responseCode = "409", description = "User is already a member")
    public ResponseEntity<Void> rejectJoinRequest(@PathVariable UUID id, @PathVariable UUID userId) {
        communityMembershipService.reject(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/dashboard")
    @Operation(summary = "Get the community home page (dashboard) payload")
    @ApiResponse(responseCode = "200", description = "Community dashboard retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Community not found")
    public ResponseEntity<CommunityDashboardDTO> getDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(communityDashboardService.getDashboard(id));
    }

    @PostMapping("/{communityId}/topics")
    @Operation(summary = "Create a topic in a community")
    @ApiResponse(responseCode = "201", description = "Topic created")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Community not found")
    public ResponseEntity<TopicSummaryDTO> createTopic(
            @PathVariable UUID communityId,
            @RequestBody @Valid CreateTopicRequest request) {
        return ResponseEntity.status(201).body(communityTopicService.createTopic(communityId, request));
    }

    @PostMapping("/topics/{topicId}/messages")
    @Operation(summary = "Post a message to a topic")
    @ApiResponse(responseCode = "201", description = "Message posted")
    @ApiResponse(responseCode = "400", description = "Validation failed (blank, over 2048 chars, or contains HTML)")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Topic not found")
    public ResponseEntity<TopicMessageDTO> postMessage(
            @PathVariable UUID topicId,
            @RequestBody @Valid CreateTopicMessageRequest request) {
        return ResponseEntity.status(201).body(communityTopicService.postMessage(topicId, request));
    }

    @GetMapping("/topics/{topicId}/messages")
    @Operation(summary = "List a topic's messages, paginated ten at a time")
    @ApiResponse(responseCode = "200", description = "Messages retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Topic not found")
    public ResponseEntity<TopicMessagesPageDTO> listMessages(
            @PathVariable UUID topicId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(communityTopicService.getMessages(topicId, page));
    }
}
