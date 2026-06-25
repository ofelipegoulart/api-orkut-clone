package com.orkutclone.api.service;

import com.orkutclone.api.dto.UpdateUserRequest;
import com.orkutclone.api.dto.UserResponse;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser() {
        return toResponse(authenticatedUser());
    }

    private static final int MAX_BIO_LENGTH = 1024;

    public UserResponse updateUser(UpdateUserRequest request) {
        User user = userRepository.findById(authenticatedUser().getId()).orElseThrow();

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
            }
            user.setName(request.name());
        }
        if (request.bio() != null) {
            if (request.bio().length() > MAX_BIO_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Bio exceeds maximum length of " + MAX_BIO_LENGTH + " characters");
            }
            user.setBio(request.bio());
        }
        if (request.profilePicture() != null) user.setProfilePicture(request.profilePicture());
        if (request.birthDate() != null) {
            if (request.birthDate().isAfter(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Birth date cannot be in the future");
            }
            user.setBirthDate(request.birthDate());
        }

        return toResponse(userRepository.save(user));
    }

    private User authenticatedUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getBio(),
                user.getProfilePicture(),
                user.getProfilePicture(),
                user.getBirthDate(),
                user.getCreatedAt() != null ? user.getCreatedAt().atOffset(ZoneOffset.UTC) : null
        );
    }
}
