package com.orkutclone.api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("201 — registro válido retorna token JWT e dados do usuário")
        void shouldRegisterSuccessfully() throws Exception {
            String body = """
                    {"name": "Felipe Goulart", "email": "felipe_reg@orkut.com", "password": "senha123"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.name").value("Felipe Goulart"))
                    .andExpect(jsonPath("$.email").value("felipe_reg@orkut.com"))
                    .andExpect(jsonPath("$.userId").isNotEmpty());
        }

        @Test
        @DisplayName("409 — email duplicado retorna conflict")
        void shouldRejectDuplicateEmail() throws Exception {
            String body = """
                    {"name": "Original", "email": "duplicado@orkut.com", "password": "senha123"}
                    """;
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Email already in use"));
        }

        @Test
        @DisplayName("400 — nome em branco falha na validação do DTO")
        void shouldRejectBlankName() throws Exception {
            String body = """
                    {"name": "", "email": "blank@orkut.com", "password": "senha123"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"));
        }

        @Test
        @DisplayName("400 — email inválido falha na validação")
        void shouldRejectInvalidEmail() throws Exception {
            String body = """
                    {"name": "Felipe", "email": "nao-eh-email", "password": "senha123"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — senha menor que 6 caracteres falha na validação")
        void shouldRejectShortPassword() throws Exception {
            String body = """
                    {"name": "Felipe", "email": "short@orkut.com", "password": "12345"}
                    """;

            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — body vazio retorna erro")
        void shouldRejectEmptyBody() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("200 — login com credenciais corretas retorna token")
        void shouldLoginSuccessfully() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Login User", "email": "login@orkut.com", "password": "senha123"}
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "login@orkut.com", "password": "senha123"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.type").value("Bearer"))
                    .andExpect(jsonPath("$.name").value("Login User"));
        }

        @Test
        @DisplayName("401 — senha incorreta retorna unauthorized")
        void shouldRejectWrongPassword() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Wrong Pass", "email": "wrongpass@orkut.com", "password": "senha123"}
                                    """))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "wrongpass@orkut.com", "password": "senhaErrada"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Email or password is incorrect"));
        }

        @Test
        @DisplayName("401 — email que não existe retorna unauthorized")
        void shouldRejectNonExistentEmail() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email": "naoexiste@orkut.com", "password": "senha123"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Segurança — Acesso sem autenticação")
    class SecurityAccess {

        @Test
        @DisplayName("Endpoints /auth/** são públicos (não precisam de token)")
        void authEndpointsShouldBePublic() throws Exception {
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Public", "email": "public@orkut.com", "password": "senha123"}
                                    """))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Endpoints protegidos retornam 403 sem token")
        void protectedEndpointsShouldRequireToken() throws Exception {
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .get("/users/me")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Token inválido retorna 403")
        void invalidTokenShouldBeForbidden() throws Exception {
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .get("/users/me")
                                    .header("Authorization", "Bearer token.invalido.aqui")
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Token válido permite acesso aos endpoints protegidos")
        void validTokenShouldGrantAccess() throws Exception {
            var auth = registerUniqueUser();

            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .get("/users/me")
                                    .header("Authorization", authHeader(auth))
                                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(auth.email()));
        }
    }
}
