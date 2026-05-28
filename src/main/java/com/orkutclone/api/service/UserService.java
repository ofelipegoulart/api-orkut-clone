package com.orkutclone.api.service;

import com.orkutclone.api.dto.UpdateUserRequest;
import com.orkutclone.api.dto.UserResponse;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser() {
        return toResponse(authenticatedUser());
    }

    public UserResponse updateUser(UpdateUserRequest request) {
        User user = userRepository.findById(authenticatedUser().getId()).orElseThrow();

        if (request.name() != null) user.setName(request.name());
        if (request.bio() != null) user.setBio(request.bio());
        if (request.profilePicture() != null) user.setProfilePicture(request.profilePicture());
        if (request.birthDate() != null) user.setBirthDate(request.birthDate());

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
                user.getBirthDate(),
                user.getCreatedAt()
        );
    }
}
