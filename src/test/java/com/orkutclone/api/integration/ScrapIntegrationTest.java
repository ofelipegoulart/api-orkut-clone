package com.orkutclone.api.integration;

import com.orkutclone.api.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ScrapIntegrationTest extends BaseIntegrationTest {

    private AuthResponse felipe;
    private AuthResponse maria;

    @BeforeEach
    void setUp() throws Exception {
        felipe = registerUniqueUser();
        maria = registerUniqueUser();
    }

    private String createScrapJson(String content, UUID ownerId) {
        return """
                {"content": "%s", "ownerId": "%s"}
                """.formatted(content, ownerId);
    }

    private UUID createScrapAndGetId(AuthResponse author, UUID ownerId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/scraps")
                        .header("Authorization", authHeader(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createScrapJson(content, ownerId)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText().transform(UUID::fromString);
    }

    @Nested
    @DisplayName("POST /scraps — Criar recado")
    class CreateScrap {

        @Test
        @DisplayName("201 — Felipe cria recado no mural da Maria")
        void shouldCreateScrapSuccessfully() throws Exception {
            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createScrapJson("Oi Maria!", maria.userId())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("Oi Maria!"))
                    .andExpect(jsonPath("$.authorId").value(felipe.userId().toString()))
                    .andExpect(jsonPath("$.authorName").value(felipe.name()))
                    .andExpect(jsonPath("$.ownerId").value(maria.userId().toString()))
                    .andExpect(jsonPath("$.readAt").isEmpty())
                    .andExpect(jsonPath("$.parentId").isEmpty());
        }

        @Test
        @DisplayName("400 — não pode enviar recado para si mesmo")
        void shouldRejectSelfScrap() throws Exception {
            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createScrapJson("Pra mim mesmo", felipe.userId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — conteúdo vazio falha na validação do DTO")
        void shouldRejectEmptyContent() throws Exception {
            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "", "ownerId": "%s"}
                                    """.formatted(maria.userId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — conteúdo com mais de 1024 caracteres é rejeitado")
        void shouldRejectContentOver1024Chars() throws Exception {
            String longContent = "a".repeat(1025);
            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "%s", "ownerId": "%s"}
                                    """.formatted(longContent, maria.userId())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve sanitizar HTML — remove <script> mas mantém <b>")
        void shouldSanitizeHtml() throws Exception {
            String html = "<b>Oi!</b><script>alert('xss')</script>";
            MvcResult result = mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "%s", "ownerId": "%s"}
                                    """.formatted(html, maria.userId())))
                    .andExpect(status().isCreated())
                    .andReturn();

            String content = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("content").asText();
            assert content.contains("<b>Oi!</b>");
            assert !content.contains("<script>");
        }

        @Test
        @DisplayName("403 — sem token retorna forbidden")
        void shouldRequireAuthentication() throws Exception {
            mockMvc.perform(post("/scraps")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createScrapJson("Sem auth", maria.userId())))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /users/{userId}/scraps — Listar mural")
    class ListWall {

        @Test
        @DisplayName("Deve listar recados do mural com paginação")
        void shouldListScrapsOnWall() throws Exception {
            createScrapAndGetId(felipe, maria.userId(), "Recado 1");
            createScrapAndGetId(felipe, maria.userId(), "Recado 2");

            mockMvc.perform(get("/users/{userId}/scraps", maria.userId())
                            .param("size", "10")
                            .header("Authorization", authHeader(felipe)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("400 — page size 25 é rejeitado (só aceita 10, 20, 50)")
        void shouldRejectInvalidPageSize() throws Exception {
            mockMvc.perform(get("/users/{userId}/scraps", maria.userId())
                            .param("size", "25")
                            .header("Authorization", authHeader(felipe)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Scrap privado não aparece para terceiros na listagem do mural")
        void shouldHidePrivateScrapsFromThirdParty() throws Exception {
            AuthResponse joao = registerUniqueUser();

            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "Privado", "ownerId": "%s", "isPrivate": true}
                                    """.formatted(maria.userId())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/users/{userId}/scraps", maria.userId())
                            .param("size", "10")
                            .header("Authorization", authHeader(joao)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        @DisplayName("Dono do mural vê todos os recados, incluindo privados")
        void ownerSeesAllScraps() throws Exception {
            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "Privado pra Maria", "ownerId": "%s", "isPrivate": true}
                                    """.formatted(maria.userId())))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/users/{userId}/scraps", maria.userId())
                            .param("size", "10")
                            .header("Authorization", authHeader(maria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("DELETE /scraps — Delete em lote e individual")
    class DeleteScraps {

        @Test
        @DisplayName("204 — autor pode deletar seu próprio scrap")
        void authorCanDelete() throws Exception {
            UUID scrapId = createScrapAndGetId(felipe, maria.userId(), "Deletar");

            mockMvc.perform(delete("/scraps/{id}", scrapId)
                            .header("Authorization", authHeader(felipe)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/scraps/{id}", scrapId)
                            .header("Authorization", authHeader(felipe)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("204 — dono do mural pode deletar scrap no seu mural")
        void wallOwnerCanDelete() throws Exception {
            UUID scrapId = createScrapAndGetId(felipe, maria.userId(), "Deletar do mural");

            mockMvc.perform(delete("/scraps/{id}", scrapId)
                            .header("Authorization", authHeader(maria)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("403 — terceiro não pode deletar scrap alheio")
        void thirdPartyCannotDelete() throws Exception {
            AuthResponse joao = registerUniqueUser();
            UUID scrapId = createScrapAndGetId(felipe, maria.userId(), "Não é do João");

            mockMvc.perform(delete("/scraps/{id}", scrapId)
                            .header("Authorization", authHeader(joao)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("204 — delete em lote funciona para scraps autorizados")
        void shouldBulkDelete() throws Exception {
            UUID id1 = createScrapAndGetId(felipe, maria.userId(), "Lote 1");
            UUID id2 = createScrapAndGetId(felipe, maria.userId(), "Lote 2");

            mockMvc.perform(delete("/scraps")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    ["%s", "%s"]
                                    """.formatted(id1, id2)))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("PATCH /scraps/mark-read + GET /scraps/unread-count")
    class ReadStatus {

        @Test
        @DisplayName("Mark-read marca scraps como lidos e unread-count reflete")
        void shouldMarkAsReadAndCountUnread() throws Exception {
            createScrapAndGetId(felipe, maria.userId(), "Não lido 1");
            createScrapAndGetId(felipe, maria.userId(), "Não lido 2");

            mockMvc.perform(get("/scraps/unread-count")
                            .param("ownerId", maria.userId().toString())
                            .header("Authorization", authHeader(maria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(2));

            MvcResult wallResult = mockMvc.perform(get("/users/{userId}/scraps", maria.userId())
                            .param("size", "10")
                            .header("Authorization", authHeader(maria)))
                    .andReturn();

            var scraps = objectMapper.readTree(wallResult.getResponse().getContentAsString())
                    .get("content");
            String id1 = scraps.get(0).get("id").asText();
            String id2 = scraps.get(1).get("id").asText();

            mockMvc.perform(patch("/scraps/mark-read")
                            .header("Authorization", authHeader(maria))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ids": ["%s", "%s"]}
                                    """.formatted(id1, id2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.markedAsRead").value(2));

            mockMvc.perform(get("/scraps/unread-count")
                            .param("ownerId", maria.userId().toString())
                            .header("Authorization", authHeader(maria)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(0));
        }

        @Test
        @DisplayName("Não pode marcar como lido scraps do mural de outra pessoa")
        void shouldNotMarkOtherUsersScrapAsRead() throws Exception {
            UUID scrapId = createScrapAndGetId(felipe, maria.userId(), "No mural da Maria");

            mockMvc.perform(patch("/scraps/mark-read")
                            .header("Authorization", authHeader(felipe))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"ids": ["%s"]}
                                    """.formatted(scrapId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.markedAsRead").value(0));
        }
    }

    @Nested
    @DisplayName("GET /scraps/{id}/thread — Fio de conversa")
    class Thread {

        @Test
        @DisplayName("Deve retornar thread completa com root + replies")
        void shouldReturnFullThread() throws Exception {
            UUID rootId = createScrapAndGetId(felipe, maria.userId(), "Oi Maria!");

            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(maria))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "Oi Felipe!", "ownerId": "%s", "parentId": "%s"}
                                    """.formatted(felipe.userId(), rootId)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/scraps/{id}/thread", rootId)
                            .header("Authorization", authHeader(felipe)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].content").value("Oi Maria!"))
                    .andExpect(jsonPath("$[0].parentId").isEmpty())
                    .andExpect(jsonPath("$[1].content").value("Oi Felipe!"))
                    .andExpect(jsonPath("$[1].parentId").value(rootId.toString()));
        }
    }
}
