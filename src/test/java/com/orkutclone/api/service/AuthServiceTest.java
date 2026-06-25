package com.orkutclone.api.service;

import com.orkutclone.api.dto.AuthResponse;
import com.orkutclone.api.dto.LoginRequest;
import com.orkutclone.api.dto.RegisterRequest;
import com.orkutclone.api.exception.ConflictException;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.Role;
import com.orkutclone.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("Felipe Goulart")
                .email("felipe@orkut.com")
                .password("encoded-password")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("Registro - Criando minha conta no Orkut")
    class Register {

        @Test
        @DisplayName("Deve criar conta com sucesso e receber token de acesso")
        void shouldRegisterSuccessfully() {
            RegisterRequest request = new RegisterRequest("Felipe Goulart", "felipe@orkut.com", "minhaSenha123");

            when(userRepository.existsByEmail("felipe@orkut.com")).thenReturn(false);
            when(passwordEncoder.encode("minhaSenha123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(savedUser.getId());
                return u;
            });
            when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-gerado");

            AuthResponse response = authService.register(request);

            assertThat(response.token()).isEqualTo("jwt-token-gerado");
            assertThat(response.type()).isEqualTo("Bearer");
            assertThat(response.name()).isEqualTo("Felipe Goulart");
            assertThat(response.email()).isEqualTo("felipe@orkut.com");
            assertThat(response.userId()).isEqualTo(savedUser.getId());
        }

        @Test
        @DisplayName("Deve salvar o usuário com senha criptografada e role USER")
        void shouldSaveUserWithEncodedPasswordAndUserRole() {
            RegisterRequest request = new RegisterRequest("Felipe Goulart", "felipe@orkut.com", "minhaSenha123");

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("minhaSenha123")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(any(User.class))).thenReturn("token");

            authService.register(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User captured = captor.getValue();

            assertThat(captured.getPassword()).isEqualTo("encoded-password");
            assertThat(captured.getRole()).isEqualTo(Role.USER);
            assertThat(captured.getName()).isEqualTo("Felipe Goulart");
        }

        @Test
        @DisplayName("Não deve permitir registrar com email que já existe")
        void shouldRejectDuplicateEmail() {
            RegisterRequest request = new RegisterRequest("Outro Felipe", "felipe@orkut.com", "senha123");

            when(userRepository.existsByEmail("felipe@orkut.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Email already in use");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Login - Entrando na minha conta")
    class Login {

        @Test
        @DisplayName("Deve fazer login com credenciais corretas e receber token")
        void shouldLoginSuccessfully() {
            LoginRequest request = new LoginRequest("felipe@orkut.com", "minhaSenha123");

            when(userRepository.findByEmail("felipe@orkut.com")).thenReturn(Optional.of(savedUser));
            when(jwtService.generateToken(savedUser)).thenReturn("jwt-token-login");

            AuthResponse response = authService.login(request);

            assertThat(response.token()).isEqualTo("jwt-token-login");
            assertThat(response.type()).isEqualTo("Bearer");
            assertThat(response.name()).isEqualTo("Felipe Goulart");
            assertThat(response.email()).isEqualTo("felipe@orkut.com");

            verify(authenticationManager).authenticate(
                    new UsernamePasswordAuthenticationToken("felipe@orkut.com", "minhaSenha123")
            );
        }

        @Test
        @DisplayName("Deve rejeitar login com senha incorreta")
        void shouldRejectWrongPassword() {
            LoginRequest request = new LoginRequest("felipe@orkut.com", "senhaErrada");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
