package com.orkutclone.api.controller;

import com.orkutclone.api.dto.CreateScrapRequest;
import com.orkutclone.api.dto.ScrapResponse;
import com.orkutclone.api.dto.UpdateScrapRequest;
import com.orkutclone.api.service.ScrapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping("/scraps")
    public ResponseEntity<ScrapResponse> create(@RequestBody @Valid CreateScrapRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scrapService.create(request));
    }

    @GetMapping("/scraps/sent")
    public ResponseEntity<Page<ScrapResponse>> findSent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(scrapService.findSent(page, size));
    }

    @GetMapping("/scraps/{id}")
    public ResponseEntity<ScrapResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(scrapService.findById(id));
    }

    @PutMapping("/scraps/{id}")
    public ResponseEntity<ScrapResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateScrapRequest request) {
        return ResponseEntity.ok(scrapService.update(id, request));
    }

    @DeleteMapping("/scraps/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        scrapService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/scraps")
    public ResponseEntity<Page<ScrapResponse>> findByOwner(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return ResponseEntity.ok(scrapService.findByOwner(userId, page, size));
    }
}
