package com.orkutclone.api.integration;

import com.orkutclone.api.dto.AuthResponse;
import com.orkutclone.api.dto.CreateScrapRequest;
import com.orkutclone.api.dto.ScrapResponse;
import com.orkutclone.api.dto.community.CommunityDetailDTO;
import com.orkutclone.api.dto.community.CreateCommunityRequest;
import com.orkutclone.api.dto.profile.CreateProfileRatingRequest;
import com.orkutclone.api.dto.profile.CreateTestimonialRequest;
import com.orkutclone.api.dto.profile.FriendRequestDTO;
import com.orkutclone.api.model.enums.CommunityCategory;
import com.orkutclone.api.model.enums.CommunityContentPrivacy;
import com.orkutclone.api.model.enums.CommunityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

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
    @DisplayName("PUT /users/me/status — Frase de status")
    class UpdateStatusMessage {

        @Test
        @DisplayName("200 — adiciona frase de status quando não havia nenhuma")
        void shouldAddStatusMessage() throws Exception {
            mockMvc.perform(put("/users/me/status")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"statusMessage": "Curtindo o Orkut de novo!"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusMessage").value("Curtindo o Orkut de novo!"));

            mockMvc.perform(get("/users/me")
                            .header("Authorization", authHeader(user)))
                    .andExpect(jsonPath("$.statusMessage").value("Curtindo o Orkut de novo!"));
        }

        @Test
        @DisplayName("200 — edita frase de status existente")
        void shouldEditStatusMessage() throws Exception {
            mockMvc.perform(put("/users/me/status")
                    .header("Authorization", authHeader(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"statusMessage": "Status original"}
                            """));

            mockMvc.perform(put("/users/me/status")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"statusMessage": "Status atualizado"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusMessage").value("Status atualizado"));
        }

        @Test
        @DisplayName("200 — envio de null limpa a frase de status")
        void shouldClearStatusMessage() throws Exception {
            mockMvc.perform(put("/users/me/status")
                    .header("Authorization", authHeader(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"statusMessage": "Status original"}
                            """));

            mockMvc.perform(put("/users/me/status")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"statusMessage": null}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusMessage").isEmpty());
        }

        @Test
        @DisplayName("400 — frase de status com mais de 140 caracteres é rejeitada")
        void shouldRejectLongStatusMessage() throws Exception {
            String longStatus = "a".repeat(141);
            mockMvc.perform(put("/users/me/status")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"statusMessage": "%s"}
                                    """.formatted(longStatus)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("403 — sem token retorna forbidden")
        void shouldRequireAuth() throws Exception {
            mockMvc.perform(put("/users/me/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"statusMessage": "Status"}
                                    """))
                    .andExpect(status().isForbidden());
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

    @Nested
    @DisplayName("DELETE /users/me — Excluir conta")
    class DeleteAccount {

        @Test
        @DisplayName("204 — conta com dados relacionados (scraps, amigos, comunidade, testemunho, avaliação) é excluída sem violar FKs, some da busca e comunidade sobrevive sem dono")
        void shouldDeleteAccountWithRelatedDataAndDisappearFromSearch() throws Exception {
            AuthResponse other = registerUniqueUser();
            AuthResponse third = registerUniqueUser();

            // Comunidade criada e possuída por `user`; `other` entra como membro.
            var communityRequest = new CreateCommunityRequest(
                    "Comunidade do Felipe " + System.nanoTime(),
                    CommunityCategory.OTHERS, CommunityType.PUBLIC, CommunityContentPrivacy.OPEN_TO_NON_MEMBERS,
                    null, null, null, null, null);
            MvcResult communityResult = mockMvc.perform(post("/api/community")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(communityRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();
            CommunityDetailDTO community = objectMapper.readValue(
                    communityResult.getResponse().getContentAsString(), CommunityDetailDTO.class);

            mockMvc.perform(post("/api/community/{id}/join", community.id())
                            .header("Authorization", authHeader(other)))
                    .andExpect(status().isOk());

            // `user` escreve no mural de `other`; `third` responde por baixo, no mesmo mural — a
            // resposta deve sobreviver com parentId nulo depois que o scrap de `user` for apagado.
            MvcResult rootScrapResult = mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateScrapRequest("Oi!", other.userId(), false, null))))
                    .andExpect(status().isCreated())
                    .andReturn();
            ScrapResponse rootScrap = objectMapper.readValue(
                    rootScrapResult.getResponse().getContentAsString(), ScrapResponse.class);

            MvcResult replyScrapResult = mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(third))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateScrapRequest("Oi também!", other.userId(), false, rootScrap.id()))))
                    .andExpect(status().isCreated())
                    .andReturn();
            ScrapResponse replyScrap = objectMapper.readValue(
                    replyScrapResult.getResponse().getContentAsString(), ScrapResponse.class);

            // Amizade entre `user` e `other` (gera profile_statistics para ambos).
            MvcResult friendRequestResult = mockMvc.perform(post("/api/profile/friends/{friendUserId}", user.userId())
                            .header("Authorization", authHeader(other)))
                    .andExpect(status().isCreated())
                    .andReturn();
            FriendRequestDTO friendRequest = objectMapper.readValue(
                    friendRequestResult.getResponse().getContentAsString(), FriendRequestDTO.class);
            mockMvc.perform(post("/api/profile/friends/requests/{requestId}/accept", friendRequest.requestId())
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isNoContent());

            // Testemunho e avaliação recebidos por `user`.
            mockMvc.perform(post("/api/profile/testimonials/{targetUserId}", user.userId())
                            .header("Authorization", authHeader(other))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateTestimonialRequest("Ótima pessoa!"))))
                    .andExpect(status().isCreated());
            mockMvc.perform(post("/api/profile/ratings/{targetUserId}", user.userId())
                            .header("Authorization", authHeader(other))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateProfileRatingRequest(5, 5, 5))))
                    .andExpect(status().isNoContent());

            // A exclusão em si: antes da correção, isso estourava violação de FK e a conta nunca
            // saía do banco (por isso continuava aparecendo na busca).
            mockMvc.perform(delete("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"password": "senha123", "confirm": true}
                                    """))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/users/{id}", user.userId())
                            .header("Authorization", authHeader(other)))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get("/search")
                            .param("q", user.name())
                            .header("Authorization", authHeader(other)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[?(@.email == '" + user.email() + "')]").doesNotExist());

            // A comunidade sobrevive sem dono, não é apagada em cascata.
            mockMvc.perform(get("/search")
                            .param("q", community.name())
                            .header("Authorization", authHeader(other)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[?(@.name == '" + community.name() + "')].resultType")
                            .value(hasItem("COMMUNITY")));

            // A resposta de `other` ao scrap apagado de `user` sobrevive, sem o parent pendurado.
            mockMvc.perform(get("/scraps/{id}", replyScrap.id())
                            .header("Authorization", authHeader(other)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parentId").isEmpty());
        }

        @Test
        @DisplayName("401 — senha incorreta não exclui a conta")
        void shouldRejectWrongPassword() throws Exception {
            mockMvc.perform(delete("/users/me")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"password": "senhaErrada", "confirm": true}
                                    """))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/users/me")
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("403 — sem token retorna forbidden")
        void shouldRequireAuth() throws Exception {
            mockMvc.perform(delete("/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"password": "senha123", "confirm": true}
                                    """))
                    .andExpect(status().isForbidden());
        }
    }
}
