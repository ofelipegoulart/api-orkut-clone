package com.orkutclone.api.integration;

import com.orkutclone.api.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

class ProfileIntegrationTest extends BaseIntegrationTest {

    private AuthResponse user;

    @BeforeEach
    void setUp() throws Exception {
        user = registerUniqueUser();
    }

    @Nested
    @DisplayName("GET/PATCH /api/profile/general — Perfil geral")
    class GeneralProfile {

        @Test
        @DisplayName("GET cria o perfil automaticamente na primeira vez (lazy creation)")
        void shouldAutoCreateOnFirstAccess() throws Exception {
            mockMvc.perform(get("/api/profile/general")
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").isEmpty())
                    .andExpect(jsonPath("$.languages").isArray())
                    .andExpect(jsonPath("$.languages", hasSize(0)));
        }

        @Test
        @DisplayName("PATCH atualiza nome e sincroniza com User.name")
        void shouldUpdateAndSyncName() throws Exception {
            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Felipe", "lastName": "Silva"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Felipe"))
                    .andExpect(jsonPath("$.lastName").value("Silva"));

            mockMvc.perform(get("/users/me")
                            .header("Authorization", authHeader(user)))
                    .andExpect(jsonPath("$.name").value("Felipe Silva"));
        }

        @Test
        @DisplayName("400 — gênero inválido é rejeitado")
        void shouldRejectInvalidGender() throws Exception {
            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"gender": "helicóptero"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve aceitar os 3 gêneros válidos")
        void shouldAcceptValidGenders() throws Exception {
            for (String gender : new String[]{"masculino", "feminino", "não binário"}) {
                mockMvc.perform(patch("/api/profile/general")
                                .header("Authorization", authHeader(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"gender": "%s"}
                                        """.formatted(gender)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.gender").value(gender));
            }
        }

        @Test
        @DisplayName("400 — país inexistente é rejeitado")
        void shouldRejectInvalidCountry() throws Exception {
            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"country": "Nárnia"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve aceitar país válido: Brasil")
        void shouldAcceptValidCountry() throws Exception {
            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"country": "Brasil"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.country").value("Brasil"));
        }

        @Test
        @DisplayName("400 — idioma inválido na lista é rejeitado")
        void shouldRejectInvalidLanguage() throws Exception {
            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"languages": ["Português", "Klingon"]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — mês de nascimento inválido é rejeitado")
        void shouldRejectInvalidBirthMonth() throws Exception {
            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"birthMonth": "Fevreiro"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Campos null não alteram valores existentes (update parcial)")
        void shouldPreserveExistingFieldsOnPartialUpdate() throws Exception {
            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"firstName": "Felipe", "country": "Brasil"}
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(patch("/api/profile/general")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"city": "São Paulo"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Felipe"))
                    .andExpect(jsonPath("$.country").value("Brasil"))
                    .andExpect(jsonPath("$.city").value("São Paulo"));
        }
    }

    @Nested
    @DisplayName("GET /api/profile/overview — Perfil completo")
    class ProfileOverview {

        @Test
        @DisplayName("Deve carregar o overview do usuário autenticado em uma única resposta")
        void shouldLoadAuthenticatedUserOverview() throws Exception {
            mockMvc.perform(get("/api/profile/overview")
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(user.userId().toString()))
                    .andExpect(jsonPath("$.user.name").value("User 1"))
                    .andExpect(jsonPath("$.shortcuts.scrapsCount").value(0))
                    .andExpect(jsonPath("$.friends", hasSize(0)))
                    .andExpect(jsonPath("$.communities", hasSize(0)))
                    .andExpect(jsonPath("$.testimonialsReceived", hasSize(0)));
        }

        @Test
        @DisplayName("Deve carregar o overview de outro usuário pelo userId")
        void shouldLoadOtherUserOverview() throws Exception {
            AuthResponse other = registerUniqueUser();

            mockMvc.perform(get("/api/profile/overview")
                            .header("Authorization", authHeader(user))
                            .param("userId", other.userId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user.id").value(other.userId().toString()));
        }
    }

    @Nested
    @DisplayName("GET/PATCH /api/profile/social — Perfil social")
    class SocialProfile {

        @Test
        @DisplayName("GET cria perfil social automaticamente")
        void shouldAutoCreate() throws Exception {
            mockMvc.perform(get("/api/profile/social")
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.humor").isArray());
        }

        @Test
        @DisplayName("PATCH com valores válidos persiste corretamente")
        void shouldUpdateWithValidValues() throws Exception {
            mockMvc.perform(patch("/api/profile/social")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "smoking": "não",
                                        "drinking": "socialmente",
                                        "humor": ["seco/sarcástico", "inteligente/sagaz"],
                                        "aboutMe": "Amo programação!"
                                    }
                                    """))
                                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.smoking").value("não"))
                    .andExpect(jsonPath("$.drinking").value("socialmente"))
                    .andExpect(jsonPath("$.humor", hasSize(2)))
                    .andExpect(jsonPath("$.aboutMe").value("Amo programação!"));
        }

        @Test
        @DisplayName("400 — humor inválido é rejeitado")
        void shouldRejectInvalidHumor() throws Exception {
            mockMvc.perform(patch("/api/profile/social")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"humor": ["inventado"]}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET/PATCH /api/profile/personal — Perfil pessoal")
    class PersonalProfile {

        @Test
        @DisplayName("PATCH com valores válidos persiste corretamente")
        void shouldUpdateWithValidValues() throws Exception {
            mockMvc.perform(patch("/api/profile/personal")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                "eyeColor": "castanhos",
                                "hairColor": "preto",
                                "bodyType": "atlético(a)",
                                "intelligence": "alta",
                                "sarcasm": "sim",
                                "attractions": ["inteligência", "sarcasmo"]
                                }
                                """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eyeColor").value("castanhos"))
                    .andExpect(jsonPath("$.attractions", hasSize(2)));
        }

        @Test
        @DisplayName("400 — cor de olho inválida é rejeitada")
        void shouldRejectInvalidEyeColor() throws Exception {
            mockMvc.perform(patch("/api/profile/personal")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"eyeColor": "roxos"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET/PATCH /api/profile/professional — Perfil profissional")
    class ProfessionalProfile {

        @Test
        @DisplayName("PATCH com escolaridade válida persiste")
        void shouldUpdateWithValidEducation() throws Exception {
            mockMvc.perform(patch("/api/profile/professional")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"education": "graduação", "company": "Google", "profession": "Dev"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.education").value("graduação"))
                    .andExpect(jsonPath("$.company").value("Google"));
        }

        @Test
        @DisplayName("400 — escolaridade inválida é rejeitada")
        void shouldRejectInvalidEducation() throws Exception {
            mockMvc.perform(patch("/api/profile/professional")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"education": "PhD em Hogwarts"}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET/PATCH /api/profile/contact — Perfil de contato")
    class ContactProfile {

        @Test
        @DisplayName("PATCH com emails secundários persiste com privacidade")
        void shouldUpdateSecondaryEmails() throws Exception {
            mockMvc.perform(patch("/api/profile/contact")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "primaryEmail": "teste@orkut.com",
                                        "primaryEmailPrivacy": "FRIENDS",
                                        "secondaryEmails": [
                                            {"email": "pessoal@gmail.com", "privacy": "ONLY_ME"}
                                        ]
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.primaryEmail").value("teste@orkut.com"))
                    .andExpect(jsonPath("$.primaryEmailPrivacy").value("FRIENDS"))
                    .andExpect(jsonPath("$.secondaryEmails", hasSize(1)))
                    .andExpect(jsonPath("$.secondaryEmails[0].email").value("pessoal@gmail.com"))
                    .andExpect(jsonPath("$.secondaryEmails[0].privacy").value("ONLY_ME"));
        }
    }

    @Nested
    @DisplayName("POST/DELETE /api/profile/avatar — Avatar")
    class Avatar {

        private MockMultipartFile validPngUpload() throws Exception {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return new MockMultipartFile("file", "avatar.png", "image/png", baos.toByteArray());
        }

        @Test
        @DisplayName("Upload de avatar válido persiste e aparece no /users/me")
        void shouldUploadAndReflectInProfile() throws Exception {
            mockMvc.perform(multipart("/api/profile/avatar")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.avatar").value(startsWith("/uploads/avatars/")))
                    .andExpect(jsonPath("$.avatar").value(endsWith(".png")));

            mockMvc.perform(get("/users/me")
                            .header("Authorization", authHeader(user)))
                    .andExpect(jsonPath("$.profilePicture").value(startsWith("/uploads/avatars/")))
                    .andExpect(jsonPath("$.avatar").value(startsWith("/uploads/avatars/")));
        }

        @Test
        @DisplayName("Delete avatar remove e /users/me reflete null")
        void shouldDeleteAndReflectInProfile() throws Exception {
            mockMvc.perform(multipart("/api/profile/avatar")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/profile/avatar")
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/users/me")
                            .header("Authorization", authHeader(user)))
                    .andExpect(jsonPath("$.profilePicture").isEmpty());
        }

        @Test
        @DisplayName("400 — arquivo que não é imagem é rejeitado")
        void shouldRejectInvalidFormat() throws Exception {
            MockMultipartFile notImage = new MockMultipartFile(
                    "file", "notes.txt", "text/plain", "isto nao eh uma imagem".getBytes());

            mockMvc.perform(multipart("/api/profile/avatar")
                            .file(notImage)
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Avatar atualizado reflete no authorAvatar dos scraps")
        void shouldReflectInScrapAuthorAvatar() throws Exception {
            AuthResponse other = registerUniqueUser();

            mockMvc.perform(multipart("/api/profile/avatar")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(user)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/scraps")
                            .header("Authorization", authHeader(user))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"content": "Com avatar!", "ownerId": "%s"}
                                    """.formatted(other.userId())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.authorAvatar").value(startsWith("/uploads/avatars/")));
        }
    }
}
