package com.orkutclone.api.controller;

import com.orkutclone.api.dto.account.AccountSettingsDTO;
import com.orkutclone.api.dto.account.UpdateAccountSettingsRequest;
import com.orkutclone.api.service.AccountSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountSettingsController {

    private final AccountSettingsService accountSettingsService;

    @GetMapping("/settings")
    @Operation(summary = "Get the current user's account settings")
    @ApiResponse(responseCode = "200", description = "Account settings retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<AccountSettingsDTO> getSettings() {
        return ResponseEntity.ok(accountSettingsService.getSettings());
    }

    @PutMapping("/settings")
    @Operation(summary = "Replace the current user's account settings")
    @ApiResponse(responseCode = "200", description = "Account settings updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<AccountSettingsDTO> updateSettings(@Valid @RequestBody UpdateAccountSettingsRequest request) {
        return ResponseEntity.ok(accountSettingsService.updateSettings(request));
    }
}
