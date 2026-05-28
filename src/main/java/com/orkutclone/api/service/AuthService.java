package com.orkutclone.api.service;

import com.orkutclone.api.dto.AuthResponse;
import com.orkutclone.api.dto.LoginRequest;
import com.orkutclone.api.dto.RegisterRequest;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.Role;
import com.orkutclone.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);

        return new AuthResponse(token, "Bearer", user.getId(), user.getName(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow();

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", user.getId(), user.getName(), user.getEmail());
    }
}
