package com.orkutclone.api.integration;

import com.orkutclone.api.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /search — pesquisa universal")
class SearchIntegrationTest extends BaseIntegrationTest {

    private AuthResponse viewer;

    @BeforeEach
    void setUp() throws Exception {
        viewer = registerUniqueUser();
        // Usuário-alvo com nome acentuado e nome do meio, para busca determinística.
        registerUser("Rafael Maurício Silva", "rafael_" + System.nanoTime() + "@orkut.com", "senha123");
    }

    private static final String TARGET = "Rafael Maurício Silva";

    @Test
    @DisplayName("200 — encontra usuário por nome (case-insensitive, parcial) com envelope de paginação")
    void shouldFindUserByName() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "rafael")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("rafael"))
                .andExpect(jsonPath("$.type").value("all"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.results[?(@.name == '" + TARGET + "')].resultType").value(hasItem("USER")))
                .andExpect(jsonPath("$.results[?(@.name == '" + TARGET + "')].email").exists());
    }

    @Test
    @DisplayName("200 — resultado de usuário traz cidade, país e o \"quem eu sou\" do perfil social")
    void shouldReturnUserProfileDetails() throws Exception {
        AuthResponse target = registerUser(
                "Beatriz Fontoura", "beatriz_" + System.nanoTime() + "@orkut.com", "senha123");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/profile/general")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader(target))
                        .content("""
                                {"city":"Salvador","country":"Brasil"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/profile/social")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader(target))
                        .content("""
                                {"aboutMe":"Adoro fotografia e viagens de trem."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/search")
                        .param("q", "Beatriz Fontoura")
                        .param("type", "users")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.name == 'Beatriz Fontoura')].city").value(hasItem("Salvador")))
                .andExpect(jsonPath("$.results[?(@.name == 'Beatriz Fontoura')].country").value(hasItem("Brasil")))
                .andExpect(jsonPath("$.results[?(@.name == 'Beatriz Fontoura')].aboutMe")
                        .value(hasItem("Adoro fotografia e viagens de trem.")));
    }

    @Test
    @DisplayName("200 — nome+sobrenome sem acento encontra perfil acentuado, ignorando nome do meio")
    void shouldMatchNameWithoutAccentsAcrossTokens() throws Exception {
        // Digitado sem acento ("Mauricio") e sem o nome do meio ("Silva").
        mockMvc.perform(get("/search")
                        .param("q", "rafael mauricio")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.name == '" + TARGET + "')].resultType").value(hasItem("USER")));
    }

    @Test
    @DisplayName("200 — tokens em ordem invertida ainda encontram o perfil")
    void shouldMatchReversedTokenOrder() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "maurício rafael")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.name == '" + TARGET + "')].resultType").value(hasItem("USER")));
    }

    @Test
    @DisplayName("400 — 'q' ausente é rejeitado")
    void shouldRejectMissingQuery() throws Exception {
        mockMvc.perform(get("/search")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 — 'q' em branco é rejeitado")
    void shouldRejectBlankQuery() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "   ")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("200 — type=communities não retorna usuários")
    void shouldRestrictByType() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "rafael")
                        .param("type", "communities")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("communities"))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.results", hasSize(0)));
    }

    @Test
    @DisplayName("200 — type=topics não retorna nada quando o termo não casa com nenhum tópico")
    void shouldAcceptTopicsGracefully() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "termoquenaoexisteemnenhumtopico")
                        .param("type", "topics")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("200 — encontra tópico pelo conteúdo de uma mensagem, com snippet, comunidade e contadores")
    void shouldFindTopicByMessageContent() throws Exception {
        String communityName = "Comunidade de Testes " + System.nanoTime();
        UUID communityId = createCommunity(communityName);
        UUID topicId = createTopic(communityId, "Discussão geral");
        postMessage(topicId, "Assunto qualquer", "Alguém já visitou a Patagônia argentina de carro?");

        mockMvc.perform(get("/search")
                        .param("q", "patagonia")
                        .param("type", "topics")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.id == '" + topicId + "')].resultType").value(hasItem("TOPIC")))
                .andExpect(jsonPath("$.results[?(@.id == '" + topicId + "')].communityId").value(hasItem(communityId.toString())))
                .andExpect(jsonPath("$.results[?(@.id == '" + topicId + "')].communityName").value(hasItem(communityName)))
                .andExpect(jsonPath("$.results[?(@.id == '" + topicId + "')].aboutMe", hasItem(containsString("Patagônia"))))
                .andExpect(jsonPath("$.results[?(@.id == '" + topicId + "')].messageCount").value(hasItem(1)))
                .andExpect(jsonPath("$.results[?(@.id == '" + topicId + "')].lastMessageAt").exists());
    }

    @Test
    @DisplayName("200 — encontra tópico pelo título quando nenhuma mensagem casa")
    void shouldFindTopicByTitle() throws Exception {
        String communityName = "Comunidade de Testes " + System.nanoTime();
        UUID communityId = createCommunity(communityName);
        String topicTitle = "Receitas de bacalhoada " + System.nanoTime();
        UUID topicId = createTopic(communityId, topicTitle);

        mockMvc.perform(get("/search")
                        .param("q", "bacalhoada")
                        .param("type", "topics")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.id == '" + topicId + "')].name").value(hasItem(topicTitle)));
    }

    @Test
    @DisplayName("200 — resultado de comunidade traz categoria, localização e nº de membros")
    void shouldReturnCommunityDetails() throws Exception {
        String communityName = "Comunidade Detalhada " + System.nanoTime();
        UUID communityId = createCommunityWithLocation(communityName);

        mockMvc.perform(get("/search")
                        .param("q", communityName)
                        .param("type", "communities")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[?(@.id == '" + communityId + "')].category").value(hasItem("MUSIC")))
                .andExpect(jsonPath("$.results[?(@.id == '" + communityId + "')].city").value(hasItem("São Paulo")))
                .andExpect(jsonPath("$.results[?(@.id == '" + communityId + "')].country").value(hasItem("Brasil")))
                .andExpect(jsonPath("$.results[?(@.id == '" + communityId + "')].memberCount").value(hasItem(1)));
    }

    private UUID createCommunity(String name) throws Exception {
        String body = """
                {
                  "name": "%s",
                  "category": "PEOPLE",
                  "type": "PUBLIC",
                  "contentPrivacy": "OPEN_TO_NON_MEMBERS",
                  "language": "Português"
                }
                """.formatted(name);

        MvcResult result = mockMvc.perform(post("/api/community")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader(viewer))
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createCommunityWithLocation(String name) throws Exception {
        String body = """
                {
                  "name": "%s",
                  "category": "MUSIC",
                  "type": "PUBLIC",
                  "contentPrivacy": "OPEN_TO_NON_MEMBERS",
                  "language": "Português",
                  "location": { "city": "São Paulo", "state": "SP", "country": "Brasil" }
                }
                """.formatted(name);

        MvcResult result = mockMvc.perform(post("/api/community")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader(viewer))
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createTopic(UUID communityId, String title) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("title", title));

        MvcResult result = mockMvc.perform(post("/api/community/" + communityId + "/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader(viewer))
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private void postMessage(UUID topicId, String subject, String message) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("subject", subject, "message", message));

        mockMvc.perform(post("/api/community/topics/" + topicId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader(viewer))
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("403 — sem token retorna forbidden")
    void shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/search").param("q", "rafael"))
                .andExpect(status().isForbidden());
    }
}
