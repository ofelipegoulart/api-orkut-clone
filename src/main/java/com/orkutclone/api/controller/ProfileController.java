package com.orkutclone.api.controller;

import com.orkutclone.api.dto.profile.*;
import com.orkutclone.api.service.ProfileService;
import com.orkutclone.api.service.ProfileOverviewService;
import com.orkutclone.api.service.ProfileFriendService;
import com.orkutclone.api.service.ProfileCommunityService;
import com.orkutclone.api.service.ProfileRatingService;
import com.orkutclone.api.service.ProfileTestimonialService;
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
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileOverviewService profileOverviewService;
    private final ProfileFriendService profileFriendService;
    private final ProfileCommunityService profileCommunityService;
    private final ProfileRatingService profileRatingService;
    private final ProfileTestimonialService profileTestimonialService;

    @GetMapping("/overview")
    @Operation(summary = "Get complete profile overview")
    @ApiResponse(responseCode = "200", description = "Profile overview retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<ProfileOverviewDTO> getOverview(@RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(profileOverviewService.getOverview(userId));
    }

    @PostMapping("/friends/{friendUserId}")
    @Operation(summary = "Send a friend request")
    @ApiResponse(responseCode = "201", description = "Friend request sent")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "409", description = "Already friends or request already exists")
    public ResponseEntity<FriendRequestDTO> sendFriendRequest(@PathVariable UUID friendUserId) {
        var request = profileFriendService.sendRequest(friendUserId);
        return ResponseEntity.status(201).body(new FriendRequestDTO(
                request.getId(),
                request.getReceiver().getId(),
                request.getReceiver().getName(),
                request.getReceiver().getProfilePicture(),
                request.getCreatedAt() == null ? null : request.getCreatedAt().atOffset(java.time.ZoneOffset.UTC)));
    }

    @GetMapping("/friends/requests")
    @Operation(summary = "List received (pending) friend requests")
    @ApiResponse(responseCode = "200", description = "Friend requests retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<java.util.List<FriendRequestDTO>> listReceivedFriendRequests() {
        return ResponseEntity.ok(profileFriendService.listReceivedRequests());
    }

    @GetMapping("/friends/requests/sent")
    @Operation(summary = "List sent friend requests")
    @ApiResponse(responseCode = "200", description = "Friend requests retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<java.util.List<FriendRequestDTO>> listSentFriendRequests() {
        return ResponseEntity.ok(profileFriendService.listSentRequests());
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    @Operation(summary = "Accept a friend request")
    @ApiResponse(responseCode = "204", description = "Friend request accepted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Friend request not found")
    public ResponseEntity<Void> acceptFriendRequest(@PathVariable UUID requestId) {
        profileFriendService.acceptRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/friends/requests/{requestId}")
    @Operation(summary = "Decline a received request or cancel a sent one")
    @ApiResponse(responseCode = "204", description = "Friend request removed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Friend request not found")
    public ResponseEntity<Void> declineOrCancelFriendRequest(@PathVariable UUID requestId) {
        profileFriendService.declineOrCancelRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/friends/{friendUserId}")
    @Operation(summary = "Remove a friend")
    @ApiResponse(responseCode = "204", description = "Friend removed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> removeFriend(@PathVariable UUID friendUserId) {
        profileFriendService.removeFriend(friendUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/communities")
    @Operation(summary = "Create a community")
    @ApiResponse(responseCode = "201", description = "Community created")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<ProfileOverviewDTO.CommunityCardDTO> createCommunity(@RequestBody @Valid CreateCommunityRequest request) {
        return ResponseEntity.status(201).body(profileCommunityService.create(request));
    }

    @PostMapping("/communities/{communityId}/join")
    @Operation(summary = "Join a community")
    @ApiResponse(responseCode = "204", description = "Joined community")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> joinCommunity(@PathVariable UUID communityId) {
        profileCommunityService.join(communityId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/communities/{communityId}/leave")
    @Operation(summary = "Leave a community")
    @ApiResponse(responseCode = "204", description = "Left community")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> leaveCommunity(@PathVariable UUID communityId) {
        profileCommunityService.leave(communityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ratings/{targetUserId}")
    @Operation(summary = "Rate a profile", description = "Rate one, two, or all three categories. Any omitted category is left unchanged; at least one must be provided.")
    @ApiResponse(responseCode = "204", description = "Profile rated")
    @ApiResponse(responseCode = "400", description = "No category provided or rating own profile")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> rateProfile(@PathVariable UUID targetUserId, @RequestBody @Valid CreateProfileRatingRequest request) {
        profileRatingService.rate(targetUserId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ratings/{targetUserId}/average")
    @Operation(summary = "Get the average ratings a profile has received")
    @ApiResponse(responseCode = "200", description = "Average ratings retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<ProfileOverviewDTO.RatingsDTO> getAverageRatings(@PathVariable UUID targetUserId) {
        return ResponseEntity.ok(profileRatingService.getAverageRatings(targetUserId));
    }

    @GetMapping("/ratings/{targetUserId}/me")
    @Operation(summary = "Get which categories the authenticated user has already rated on a profile")
    @ApiResponse(responseCode = "200", description = "Rated categories retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<MyProfileRatingDTO> getMyRatedCategories(@PathVariable UUID targetUserId) {
        return ResponseEntity.ok(profileRatingService.getMyRatedCategories(targetUserId));
    }

    @PostMapping("/testimonials/{targetUserId}")
    @Operation(summary = "Send a testimonial")
    @ApiResponse(responseCode = "201", description = "Testimonial sent")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<ProfileOverviewDTO.TestimonialDTO> sendTestimonial(
            @PathVariable UUID targetUserId,
            @RequestBody @Valid CreateTestimonialRequest request) {
        var testimonial = profileTestimonialService.send(targetUserId, request);
        return ResponseEntity.status(201).body(new ProfileOverviewDTO.TestimonialDTO(
                testimonial.getId(),
                testimonial.getAuthor().getId(),
                testimonial.getAuthor().getName(),
                testimonial.getAuthor().getProfilePicture(),
                testimonial.getMessage(),
                testimonial.getStatus().name(),
                testimonial.getCreatedAt() == null ? null : testimonial.getCreatedAt().atOffset(java.time.ZoneOffset.UTC)
        ));
    }

    @PatchMapping("/testimonials/{testimonialId}/decision")
    @Operation(summary = "Approve or reject a testimonial")
    @ApiResponse(responseCode = "204", description = "Testimonial processed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<Void> decideTestimonial(@PathVariable UUID testimonialId, @RequestBody RespondTestimonialRequest request) {
        if (request.approved()) {
            profileTestimonialService.approve(testimonialId);
        } else {
            profileTestimonialService.reject(testimonialId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/testimonials/sent")
    @Operation(summary = "List sent testimonials")
    @ApiResponse(responseCode = "200", description = "Testimonials retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<java.util.List<ProfileOverviewDTO.TestimonialDTO>> listSentTestimonials(@RequestParam UUID userId) {
        return ResponseEntity.ok(profileTestimonialService.findSent(userId));
    }

    @GetMapping("/testimonials/received")
    @Operation(summary = "List received testimonials")
    @ApiResponse(responseCode = "200", description = "Testimonials retrieved")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<java.util.List<ProfileOverviewDTO.TestimonialDTO>> listReceivedTestimonials(
            @RequestParam UUID userId,
            @RequestParam(defaultValue = "false") boolean includePending) {
        return ResponseEntity.ok(profileTestimonialService.findReceived(userId, includePending));
    }

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

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload avatar")
    @ApiResponse(responseCode = "200", description = "Avatar uploaded")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<AvatarResponse> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(profileService.uploadAvatar(file));
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
