package com.orkutclone.api.service;

import com.orkutclone.api.dto.ChangePasswordRequest;
import com.orkutclone.api.dto.DeleteAccountRequest;
import com.orkutclone.api.dto.UpdateStatusMessageRequest;
import com.orkutclone.api.dto.UpdateUserRequest;
import com.orkutclone.api.dto.UserResponse;
import com.orkutclone.api.model.User;
import com.orkutclone.api.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    public UserResponse getCurrentUser() {
        return toResponse(authenticatedUser());
    }

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(user);
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

    @CacheEvict(cacheNames = "profileOverview", allEntries = true)
    public UserResponse updateStatusMessage(UpdateStatusMessageRequest request) {
        User user = userRepository.findById(authenticatedUser().getId()).orElseThrow();
        user.setStatusMessage(request.statusMessage());
        return toResponse(userRepository.save(user));
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findById(authenticatedUser().getId()).orElseThrow();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(DeleteAccountRequest request) {
        User user = userRepository.findById(authenticatedUser().getId()).orElseThrow();

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password is incorrect");
        }

        purgeUserData(user.getId());
        userRepository.delete(user);
    }

    /**
     * Removes every row that references this user before the hard delete above, since none of
     * the FKs pointing at {@code users} cascade at the DB level. Without this, {@code delete()}
     * throws a FK violation for any account with real activity (profile, scraps, friends...),
     * the transaction rolls back, and the "deleted" account keeps showing up everywhere
     * (including search) because the row was never actually removed.
     *
     * <p>Communities the user owns are kept — {@code owner_id} is just cleared — so deleting one
     * account doesn't wipe out a community's other members. Everything else the user authored,
     * including forum topics/messages and polls (and other members' replies/votes/comments on
     * them), is deleted as part of this "right to be forgotten" style cleanup.</p>
     */
    private void purgeUserData(UUID userId) {
        execute("UPDATE scraps SET parent_id = NULL WHERE parent_id IN " +
                "(SELECT id FROM scraps WHERE author_id = :userId OR owner_id = :userId)", userId);
        execute("DELETE FROM scraps WHERE author_id = :userId OR owner_id = :userId", userId);

        execute("DELETE FROM poll_votes WHERE voter_id = :userId " +
                "OR poll_id IN (SELECT id FROM polls WHERE creator_id = :userId)", userId);
        execute("DELETE FROM poll_comments WHERE author_id = :userId " +
                "OR poll_id IN (SELECT id FROM polls WHERE creator_id = :userId)", userId);
        execute("DELETE FROM poll_options WHERE poll_id IN (SELECT id FROM polls WHERE creator_id = :userId)", userId);
        execute("DELETE FROM polls WHERE creator_id = :userId", userId);

        execute("DELETE FROM topic_messages WHERE author_id = :userId " +
                "OR topic_id IN (SELECT id FROM community_topics WHERE author_id = :userId)", userId);
        execute("DELETE FROM community_topics WHERE author_id = :userId", userId);
        execute("DELETE FROM community_memberships WHERE user_id = :userId", userId);
        execute("UPDATE communities SET owner_id = NULL WHERE owner_id = :userId", userId);

        execute("UPDATE albums SET cover_photo_id = NULL WHERE owner_id = :userId", userId);
        execute("DELETE FROM photos WHERE album_id IN (SELECT id FROM albums WHERE owner_id = :userId)", userId);
        execute("DELETE FROM albums WHERE owner_id = :userId", userId);

        execute("DELETE FROM profile_friends WHERE user_id = :userId OR friend_id = :userId", userId);
        execute("DELETE FROM profile_friend_requests WHERE requester_id = :userId OR receiver_id = :userId", userId);
        execute("DELETE FROM profile_ratings WHERE rater_id = :userId OR target_id = :userId", userId);
        execute("DELETE FROM profile_testimonials WHERE author_id = :userId OR target_id = :userId", userId);
        execute("DELETE FROM profile_statistics WHERE user_id = :userId", userId);

        execute("DELETE FROM user_profile_attractions WHERE personal_id IN " +
                "(SELECT p.id FROM user_profile_personal p JOIN user_profile up ON p.profile_id = up.id " +
                "WHERE up.user_id = :userId)", userId);
        execute("DELETE FROM user_profile_personal WHERE profile_id IN " +
                "(SELECT id FROM user_profile WHERE user_id = :userId)", userId);

        execute("DELETE FROM user_profile_professional WHERE profile_id IN " +
                "(SELECT id FROM user_profile WHERE user_id = :userId)", userId);

        execute("DELETE FROM user_profile_secondary_emails WHERE contact_id IN " +
                "(SELECT c.id FROM user_profile_contact c JOIN user_profile up ON c.profile_id = up.id " +
                "WHERE up.user_id = :userId)", userId);
        execute("DELETE FROM user_profile_contact WHERE profile_id IN " +
                "(SELECT id FROM user_profile WHERE user_id = :userId)", userId);

        execute("DELETE FROM user_profile_humor WHERE social_id IN " +
                "(SELECT s.id FROM user_profile_social s JOIN user_profile up ON s.profile_id = up.id " +
                "WHERE up.user_id = :userId)", userId);
        execute("DELETE FROM user_profile_style WHERE social_id IN " +
                "(SELECT s.id FROM user_profile_social s JOIN user_profile up ON s.profile_id = up.id " +
                "WHERE up.user_id = :userId)", userId);
        execute("DELETE FROM user_profile_social WHERE profile_id IN " +
                "(SELECT id FROM user_profile WHERE user_id = :userId)", userId);

        execute("DELETE FROM user_profile_languages WHERE general_id IN " +
                "(SELECT g.id FROM user_profile_general g JOIN user_profile up ON g.profile_id = up.id " +
                "WHERE up.user_id = :userId)", userId);
        execute("DELETE FROM user_profile_interested_in WHERE general_id IN " +
                "(SELECT g.id FROM user_profile_general g JOIN user_profile up ON g.profile_id = up.id " +
                "WHERE up.user_id = :userId)", userId);
        execute("DELETE FROM user_profile_general WHERE profile_id IN " +
                "(SELECT id FROM user_profile WHERE user_id = :userId)", userId);

        execute("DELETE FROM user_profile WHERE user_id = :userId", userId);
    }

    private void execute(String sql, UUID userId) {
        entityManager.createNativeQuery(sql).setParameter("userId", userId).executeUpdate();
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
                user.getStatusMessage(),
                user.getProfilePicture(),
                user.getProfilePicture(),
                user.getBirthDate(),
                user.getCreatedAt() != null ? user.getCreatedAt().atOffset(ZoneOffset.UTC) : null
        );
    }
}
