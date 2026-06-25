package com.orkutclone.api.integration;

import com.orkutclone.api.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserIntegrationTest extends BaseIntegrationTest {

    private AuthResponse user;

    @BeforeEach
    void setUp() throws Exception {
        user = registerUniqueUser();
    }

    @Nested
    @DisplayName("GET /users/me")
    class GetCurrentUser {

        @Test
        @DisplayName("200 — retorna dados do usuário autenticado")
        void shouldReturnCurrentUserData() throws Exception {
            mockMvc.perform(get("/users/me")
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.userId().toString()))
                    .andExpect(jsonPath("$.name").value(user.name()))
                    .andExpect(jsonPath("$.email").value(user.email()))
                    .andExpect(jsonPath("$.bio").isEmpty())
                    .andExpect(jsonPath("$.profilePicture").isEmpty())
                    .andExpect(jsonPath("$.avatar").isEmpty())
                    .andExpect(jsonPath("$.createdAt").isNotEmpty());
        }

        @Test
        @DisplayName("403 — sem token retorna forbidden")
        void shouldRequireAuth() throws Exception {
            mockMvc.perform(get("/users/me"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /users/me — Atualizar perfil")
    class UpdateUser {

        @Test
        @DisplayName("200 — atualizar nome e bio persiste no banco")
        void shouldUpdateNameAndBio() throws Exception {
            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Felipe Novo", "bio": "Saudades do Orkut!"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Felipe Novo"))
                    .andExpect(jsonPath("$.bio").value("Saudades do Orkut!"));

            mockMvc.perform(get("/users/me")
                            .header("Authorization", authHeader(user)))
                    .andExpect(jsonPath("$.name").value("Felipe Novo"))
                    .andExpect(jsonPath("$.bio").value("Saudades do Orkut!"));
        }

        @Test
        @DisplayName("200 — campos null não sobrescrevem dados existentes")
        void shouldNotOverwriteWithNull() throws Exception {
            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bio": "Bio original"}
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Nome Novo"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Nome Novo"))
                    .andExpect(jsonPath("$.bio").value("Bio original"));
        }

        @Test
        @DisplayName("200 — birthDate é serializado corretamente como ISO date")
        void shouldHandleBirthDate() throws Exception {
            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"birthDate": "1995-03-15"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.birthDate").value("1995-03-15"));
        }

        @Test
        @DisplayName("400 — nome em branco é rejeitado")
        void shouldRejectBlankName() throws Exception {
            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "   "}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — bio com mais de 1024 caracteres é rejeitada")
        void shouldRejectLongBio() throws Exception {
            String longBio = "a".repeat(1025);
            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bio": "%s"}
                                    """.formatted(longBio)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — data de nascimento no futuro é rejeitada")
        void shouldRejectFutureBirthDate() throws Exception {
            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"birthDate": "2090-01-01"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("avatar e profilePicture sempre espelham o mesmo valor")
        void avatarShouldMirrorProfilePicture() throws Exception {
            mockMvc.perform(put("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"profilePicture": "data:image/png;base64,abc"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profilePicture").value("data:image/png;base64,abc"))
                    .andExpect(jsonPath("$.avatar").value("data:image/png;base64,abc"));
        }
    }

    @Nested
    @DisplayName("GET /users/{id} — Ver perfil de outro usuário")
    class GetUserById {

        @Test
        @DisplayName("200 — retorna dados de outro usuário pelo ID")
        void shouldReturnOtherUserData() throws Exception {
            var otherUser = registerUniqueUser();

            mockMvc.perform(get("/users/{id}", otherUser.userId())
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(otherUser.userId().toString()))
                    .andExpect(jsonPath("$.name").value(otherUser.name()))
                    .andExpect(jsonPath("$.email").value(otherUser.email()));
        }

        @Test
        @DisplayName("404 — ID inexistente retorna not found")
        void shouldReturn404ForNonExistentUser() throws Exception {
            mockMvc.perform(get("/users/{id}", java.util.UUID.randomUUID())
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 — sem token retorna forbidden")
        void shouldRequireAuth() throws Exception {
            mockMvc.perform(get("/users/{id}", user.userId()))
                    .andExpect(status().isForbidden());
        }
    }
}
