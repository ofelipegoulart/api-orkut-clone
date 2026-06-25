package com.orkutclone.api.controller;

import com.orkutclone.api.dto.profile.*;
import com.orkutclone.api.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/general")
    @Operation(summary = "Get general profile")
    @ApiResponse(responseCode = "200", description = "General profile retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<GeneralProfileDTO> getGeneral() {
        return ResponseEntity.ok(profileService.getGeneral());
    }

    @PatchMapping("/general")
    @Operation(summary = "Update general profile")
    @ApiResponse(responseCode = "200", description = "General profile updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<GeneralProfileDTO> updateGeneral(@RequestBody GeneralProfileDTO request) {
        return ResponseEntity.ok(profileService.updateGeneral(request));
    }

    @GetMapping("/social")
    @Operation(summary = "Get social profile")
    @ApiResponse(responseCode = "200", description = "Social profile retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<SocialProfileDTO> getSocial() {
        return ResponseEntity.ok(profileService.getSocial());
    }

    @PatchMapping("/social")
    @Operation(summary = "Update social profile")
    @ApiResponse(responseCode = "200", description = "Social profile updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<SocialProfileDTO> updateSocial(@RequestBody SocialProfileDTO request) {
        return ResponseEntity.ok(profileService.updateSocial(request));
    }

    @GetMapping("/contact")
    @Operation(summary = "Get contact profile")
    @ApiResponse(responseCode = "200", description = "Contact profile retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<ContactProfileDTO> getContact() {
        return ResponseEntity.ok(profileService.getContact());
    }

    @PatchMapping("/contact")
    @Operation(summary = "Update contact profile")
    @ApiResponse(responseCode = "200", description = "Contact profile updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<ContactProfileDTO> updateContact(@RequestBody ContactProfileDTO request) {
        return ResponseEntity.ok(profileService.updateContact(request));
    }

    @GetMapping("/professional")
    @Operation(summary = "Get professional profile")
    @ApiResponse(responseCode = "200", description = "Professional profile retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<ProfessionalProfileDTO> getProfessional() {
        return ResponseEntity.ok(profileService.getProfessional());
    }

    @PatchMapping("/professional")
    @Operation(summary = "Update professional profile")
    @ApiResponse(responseCode = "200", description = "Professional profile updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<ProfessionalProfileDTO> updateProfessional(@RequestBody ProfessionalProfileDTO request) {
        return ResponseEntity.ok(profileService.updateProfessional(request));
    }

    @GetMapping("/personal")
    @Operation(summary = "Get personal profile")
    @ApiResponse(responseCode = "200", description = "Personal profile retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<PersonalProfileDTO> getPersonal() {
        return ResponseEntity.ok(profileService.getPersonal());
    }

    @PatchMapping("/personal")
    @Operation(summary = "Update personal profile")
    @ApiResponse(responseCode = "200", description = "Personal profile updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<PersonalProfileDTO> updatePersonal(@RequestBody PersonalProfileDTO request) {
        return ResponseEntity.ok(profileService.updatePersonal(request));
    }

    @PostMapping("/avatar")
    @Operation(summary = "Upload avatar")
    @ApiResponse(responseCode = "200", description = "Avatar uploaded")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<AvatarResponse> uploadAvatar(@Valid @RequestBody AvatarRequest request) {
        return ResponseEntity.ok(profileService.uploadAvatar(request));
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Delete avatar")
    @ApiResponse(responseCode = "204", description = "Avatar deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> deleteAvatar() {
        profileService.deleteAvatar();
        return ResponseEntity.noContent().build();
    }
}
