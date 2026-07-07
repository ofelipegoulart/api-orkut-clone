package com.orkutclone.api.integration;

import com.orkutclone.api.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("200 — type=topics é aceito e retorna vazio (sem modelo de tópico)")
    void shouldAcceptTopicsGracefully() throws Exception {
        mockMvc.perform(get("/search")
                        .param("q", "rafael")
                        .param("type", "topics")
                        .header("Authorization", authHeader(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("403 — sem token retorna forbidden")
    void shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/search").param("q", "rafael"))
                .andExpect(status().isForbidden());
    }
}
