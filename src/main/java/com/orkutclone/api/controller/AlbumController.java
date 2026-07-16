package com.orkutclone.api.controller;

import com.orkutclone.api.dto.album.*;
import com.orkutclone.api.service.AlbumService;
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
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @PostMapping
    @Operation(summary = "Create an album")
    @ApiResponse(responseCode = "201", description = "Album created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<AlbumResponse> create(@RequestBody @Valid CreateAlbumRequest request) {
        return ResponseEntity.status(201).body(albumService.create(request));
    }

    @GetMapping
    @Operation(summary = "List a user's albums", description = "FRIENDS_ONLY albums are hidden unless the caller is the owner or a friend.")
    @ApiResponse(responseCode = "200", description = "Albums retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<AlbumsPageDTO> list(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(albumService.list(userId, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get album detail with its photos")
    @ApiResponse(responseCode = "200", description = "Album retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Album not found, or hidden by privacy")
    public ResponseEntity<AlbumDetailDTO> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(albumService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an album's title, description and privacy")
    @ApiResponse(responseCode = "200", description = "Album updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the album owner")
    @ApiResponse(responseCode = "404", description = "Album not found")
    public ResponseEntity<AlbumResponse> update(@PathVariable UUID id, @RequestBody @Valid UpdateAlbumRequest request) {
        return ResponseEntity.ok(albumService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an album and all of its photos")
    @ApiResponse(responseCode = "204", description = "Album deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the album owner")
    @ApiResponse(responseCode = "404", description = "Album not found")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        albumService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a photo to an album", description = "One file per call; the front-end loops for multiple uploads.")
    @ApiResponse(responseCode = "200", description = "Photo uploaded")
    @ApiResponse(responseCode = "400", description = "Missing, oversized, or unsupported image")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the album owner")
    @ApiResponse(responseCode = "404", description = "Album not found")
    public ResponseEntity<PhotoResponse> uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(albumService.uploadPhoto(id, file));
    }

    @PatchMapping("/{id}/photos/{photoId}")
    @Operation(summary = "Edit a photo's caption")
    @ApiResponse(responseCode = "200", description = "Caption updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the album owner")
    @ApiResponse(responseCode = "404", description = "Album or photo not found")
    public ResponseEntity<PhotoResponse> updateCaption(
            @PathVariable UUID id,
            @PathVariable UUID photoId,
            @RequestBody @Valid UpdatePhotoCaptionRequest request) {
        return ResponseEntity.ok(albumService.updateCaption(id, photoId, request));
    }

    @PutMapping("/{id}/cover")
    @Operation(summary = "Set the album's cover photo")
    @ApiResponse(responseCode = "200", description = "Cover updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the album owner")
    @ApiResponse(responseCode = "404", description = "Album or photo not found")
    public ResponseEntity<AlbumResponse> setCover(@PathVariable UUID id, @RequestBody @Valid SetCoverRequest request) {
        return ResponseEntity.ok(albumService.setCover(id, request));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    @Operation(summary = "Delete a photo from an album")
    @ApiResponse(responseCode = "204", description = "Photo deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Not the album owner")
    @ApiResponse(responseCode = "404", description = "Album or photo not found")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID id, @PathVariable UUID photoId) {
        albumService.deletePhoto(id, photoId);
        return ResponseEntity.noContent().build();
    }
}
