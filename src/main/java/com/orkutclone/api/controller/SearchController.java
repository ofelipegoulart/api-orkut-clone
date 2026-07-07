package com.orkutclone.api.controller;

import com.orkutclone.api.dto.SearchResponse;
import com.orkutclone.api.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "Universal search across users and communities")
    @ApiResponse(responseCode = "200", description = "Search results retrieved")
    @ApiResponse(responseCode = "400", description = "Missing or blank 'q' parameter")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "type", required = false, defaultValue = "all") String type,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "language", required = false, defaultValue = "pt-BR") String language,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "12") int size) {
        return ResponseEntity.ok(searchService.search(query, type, location, language, page, size));
    }
}
