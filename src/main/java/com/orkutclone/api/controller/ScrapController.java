package com.orkutclone.api.controller;

import com.orkutclone.api.dto.CreateScrapRequest;
import com.orkutclone.api.dto.MarkScrapsReadRequest;
import com.orkutclone.api.dto.ScrapResponse;
import com.orkutclone.api.dto.UpdateScrapRequest;
import com.orkutclone.api.service.ScrapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping("/scraps")
    @Operation(summary = "Create a new scrap")
    @ApiResponse(responseCode = "201", description = "Scrap created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Owner not found")
    public ResponseEntity<ScrapResponse> create(@RequestBody @Valid CreateScrapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scrapService.create(request));
    }

    @GetMapping("/scraps/sent")
    @Operation(summary = "List scraps sent by current user")
    @ApiResponse(responseCode = "200", description = "Scraps retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid pagination parameters")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Page<ScrapResponse>> findSent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(scrapService.findSent(page, size));
    }

    @GetMapping("/scraps/{id}")
    @Operation(summary = "Get a scrap by ID")
    @ApiResponse(responseCode = "200", description = "Scrap retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid scrap ID")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Scrap is private")
    @ApiResponse(responseCode = "404", description = "Scrap not found")
    public ResponseEntity<ScrapResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(scrapService.findById(id));
    }

    @PutMapping("/scraps/{id}")
    @Operation(summary = "Update a scrap")
    @ApiResponse(responseCode = "200", description = "Scrap updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the author")
    @ApiResponse(responseCode = "404", description = "Scrap not found")
    public ResponseEntity<ScrapResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateScrapRequest request) {
        return ResponseEntity.ok(scrapService.update(id, request));
    }

    @DeleteMapping("/scraps/{id}")
    @Operation(summary = "Delete a scrap")
    @ApiResponse(responseCode = "204", description = "Scrap deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the author or wall owner")
    @ApiResponse(responseCode = "404", description = "Scrap not found")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scrapService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/scraps/sent/{id}")
    @Operation(summary = "Delete a scrap the current user sent")
    @ApiResponse(responseCode = "204", description = "Scrap deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the author")
    @ApiResponse(responseCode = "404", description = "Scrap not found")
    public ResponseEntity<Void> deleteOwnSent(@PathVariable UUID id) {
        scrapService.deleteOwnSent(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/scraps")
    @Operation(summary = "Delete multiple scraps")
    @ApiResponse(responseCode = "204", description = "Scraps deleted")
    @ApiResponse(responseCode = "400", description = "No scrap IDs provided")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not authorized to delete one or more scraps")
    public ResponseEntity<Void> deleteMultiple(@RequestBody List<UUID> scrapIds) {
        scrapService.deleteMultiple(scrapIds);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/scraps/{id}/thread")
    @Operation(summary = "Get the conversation thread for a scrap")
    @ApiResponse(responseCode = "200", description = "Thread retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Conversation is private")
    @ApiResponse(responseCode = "404", description = "Scrap not found")
    public ResponseEntity<List<ScrapResponse>> findThread(@PathVariable UUID id) {
        return ResponseEntity.ok(scrapService.findThread(id));
    }

    @PatchMapping("/scraps/mark-read")
    @Operation(summary = "Mark scraps as read")
    @ApiResponse(responseCode = "200", description = "Scraps marked as read")
    @ApiResponse(responseCode = "400", description = "No scrap IDs provided")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<java.util.Map<String, Integer>> markAsRead(
            @RequestBody @Valid MarkScrapsReadRequest request) {
        int updated = scrapService.markAsRead(request.ids());
        return ResponseEntity.ok(java.util.Map.of("markedAsRead", updated));
    }

    @GetMapping("/scraps/unread-count")
    @Operation(summary = "Get unread scrap count for a user")
    @ApiResponse(responseCode = "200", description = "Count retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<java.util.Map<String, Long>> getUnreadCount(
            @RequestParam UUID ownerId) {
        long count = scrapService.countUnread(ownerId);
        return ResponseEntity.ok(java.util.Map.of("unreadCount", count));
    }

    @GetMapping("/users/{userId}/scraps")
    @Operation(summary = "List scraps on a user's wall")
    @ApiResponse(responseCode = "200", description = "Scraps retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid parameters")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Page<ScrapResponse>> findByOwner(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(scrapService.findByOwner(userId, page, size));
    }
}
