package com.orkutclone.api.controller;

import com.orkutclone.api.dto.UpdateUserRequest;
import com.orkutclone.api.dto.UserResponse;
import com.orkutclone.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(request));
    }
}
