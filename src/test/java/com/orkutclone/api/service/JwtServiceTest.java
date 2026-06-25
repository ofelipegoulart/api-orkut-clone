package com.orkutclone.api.service;

import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBqd3QgdG9rZW5z");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Felipe Goulart")
                .email("felipe@orkut.com")
                .password("encoded")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("Geração de Token - Quando eu faço login")
    class TokenGeneration {

        @Test
        @DisplayName("Deve gerar um token JWT válido para o usuário")
        void shouldGenerateValidToken() {
            String token = jwtService.generateToken(user);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Deve incluir o email do usuário como subject no token")
        void shouldContainUserEmailAsSubject() {
            String token = jwtService.generateToken(user);

            String extractedEmail = jwtService.extractUsername(token);
            assertThat(extractedEmail).isEqualTo("felipe@orkut.com");
        }
    }

    @Nested
    @DisplayName("Validação de Token - Verificando se posso acessar")
    class TokenValidation {

        @Test
        @DisplayName("Token recém-gerado deve ser válido para o mesmo usuário")
        void shouldBeValidForSameUser() {
            String token = jwtService.generateToken(user);

            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("Token não deve ser válido para outro usuário")
        void shouldBeInvalidForDifferentUser() {
            String token = jwtService.generateToken(user);

            User otherUser = User.builder()
                    .id(UUID.randomUUID())
                    .email("maria@orkut.com")
                    .name("Maria")
                    .password("encoded")
                    .role(Role.USER)
                    .build();

            assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
        }

        @Test
        @DisplayName("Token expirado deve lançar exceção")
        void shouldRejectExpiredToken() {
            ReflectionTestUtils.setField(jwtService, "expiration", -1000L);

            String expiredToken = jwtService.generateToken(user);

            assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, user))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("Tokens inválidos - Tentativas de acesso com tokens ruins")
    class InvalidTokens {

        @Test
        @DisplayName("Token malformado (string aleatória) deve lançar exceção")
        void shouldRejectGarbageToken() {
            assertThatThrownBy(() -> jwtService.extractUsername("isto.nao.eh.um.token.jwt"))
                    .isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("Token completamente vazio deve lançar exceção")
        void shouldRejectEmptyToken() {
            assertThatThrownBy(() -> jwtService.extractUsername(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Token assinado com chave diferente deve lançar exceção")
        void shouldRejectTokenSignedWithDifferentKey() {
            String token = jwtService.generateToken(user);

            JwtService otherService = new JwtService();
            ReflectionTestUtils.setField(otherService, "secretKey",
                    "b3V0cmEgY2hhdmUgc2VjcmV0YSBkaWZlcmVudGUgcGFyYSB0ZXN0ZXM=");
            ReflectionTestUtils.setField(otherService, "expiration", 86400000L);

            assertThatThrownBy(() -> otherService.extractUsername(token))
                    .isInstanceOf(JwtException.class);
        }

    }
}
