package com.orkutclone.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.orkutclone.api.dto.AuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlbumIntegrationTest extends BaseIntegrationTest {

    private AuthResponse owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerUniqueUser();
    }

    private MockMultipartFile validPngUpload() throws Exception {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("file", "photo.png", "image/png", baos.toByteArray());
    }

    private String createAlbum(AuthResponse user, String title, String privacy) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/albums")
                        .header("Authorization", authHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "privacy": "%s"}
                                """.formatted(title, privacy)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    private void makeFriends(AuthResponse a, AuthResponse b) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/profile/friends/" + b.userId())
                        .header("Authorization", authHeader(a)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String requestId = node.get("requestId").asText();

        mockMvc.perform(post("/api/profile/friends/requests/" + requestId + "/accept")
                        .header("Authorization", authHeader(b)))
                .andExpect(status().isNoContent());
    }

    @Nested
    @DisplayName("POST/PUT/DELETE /api/albums — CRUD do álbum")
    class AlbumCrud {

        @Test
        @DisplayName("Cria um álbum com valores padrão")
        void shouldCreateAlbum() throws Exception {
            mockMvc.perform(post("/api/albums")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Viagem", "description": "Férias 2026", "privacy": "PUBLIC"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Viagem"))
                    .andExpect(jsonPath("$.description").value("Férias 2026"))
                    .andExpect(jsonPath("$.privacy").value("PUBLIC"))
                    .andExpect(jsonPath("$.photoCount").value(0))
                    .andExpect(jsonPath("$.coverPhotoUrl").isEmpty())
                    .andExpect(jsonPath("$.ownerId").value(owner.userId().toString()));
        }

        @Test
        @DisplayName("400 — título em branco é rejeitado")
        void shouldRejectBlankTitle() throws Exception {
            mockMvc.perform(post("/api/albums")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "", "privacy": "PUBLIC"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Um usuário pode ter múltiplos álbuns")
        void shouldSupportMultipleAlbumsPerUser() throws Exception {
            createAlbum(owner, "Álbum 1", "PUBLIC");
            createAlbum(owner, "Álbum 2", "PUBLIC");

            mockMvc.perform(get("/api/albums")
                            .header("Authorization", authHeader(owner))
                            .param("userId", owner.userId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.results", hasSize(2)));
        }

        @Test
        @DisplayName("PUT atualiza título, descrição e privacidade")
        void shouldUpdateAlbum() throws Exception {
            String albumId = createAlbum(owner, "Original", "PUBLIC");

            mockMvc.perform(put("/api/albums/" + albumId)
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Atualizado", "description": "Nova descrição", "privacy": "FRIENDS_ONLY"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Atualizado"))
                    .andExpect(jsonPath("$.privacy").value("FRIENDS_ONLY"));
        }

        @Test
        @DisplayName("403 — outro usuário não pode editar o álbum")
        void shouldRejectEditByNonOwner() throws Exception {
            String albumId = createAlbum(owner, "Original", "PUBLIC");
            AuthResponse stranger = registerUniqueUser();

            mockMvc.perform(put("/api/albums/" + albumId)
                            .header("Authorization", authHeader(stranger))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Hackeado", "privacy": "PUBLIC"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE remove o álbum e suas fotos")
        void shouldDeleteAlbumAndItsPhotos() throws Exception {
            String albumId = createAlbum(owner, "Para apagar", "PUBLIC");

            mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isOk());

            mockMvc.perform(delete("/api/albums/" + albumId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/albums/" + albumId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404 — álbum inexistente")
        void shouldReturnNotFoundForMissingAlbum() throws Exception {
            mockMvc.perform(get("/api/albums/" + java.util.UUID.randomUUID())
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Privacidade — PUBLIC vs FRIENDS_ONLY")
    class Privacy {

        @Test
        @DisplayName("Álbum FRIENDS_ONLY não aparece na listagem para quem não é amigo")
        void shouldHideFriendsOnlyAlbumFromStranger() throws Exception {
            createAlbum(owner, "Público", "PUBLIC");
            createAlbum(owner, "Privado", "FRIENDS_ONLY");
            AuthResponse stranger = registerUniqueUser();

            mockMvc.perform(get("/api/albums")
                            .header("Authorization", authHeader(stranger))
                            .param("userId", owner.userId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.results[0].title").value("Público"));
        }

        @Test
        @DisplayName("Álbum FRIENDS_ONLY aparece na listagem para amigos")
        void shouldShowFriendsOnlyAlbumToFriend() throws Exception {
            createAlbum(owner, "Público", "PUBLIC");
            createAlbum(owner, "Privado", "FRIENDS_ONLY");
            AuthResponse friend = registerUniqueUser();
            makeFriends(owner, friend);

            mockMvc.perform(get("/api/albums")
                            .header("Authorization", authHeader(friend))
                            .param("userId", owner.userId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("GET /{id} de álbum FRIENDS_ONLY retorna 404 para quem não é amigo")
        void shouldHideFriendsOnlyAlbumDetailFromStranger() throws Exception {
            String albumId = createAlbum(owner, "Privado", "FRIENDS_ONLY");
            AuthResponse stranger = registerUniqueUser();

            mockMvc.perform(get("/api/albums/" + albumId)
                            .header("Authorization", authHeader(stranger)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Fotos — upload, capa, legenda e remoção")
    class Photos {

        @Test
        @DisplayName("Upload define a primeira foto como capa automaticamente")
        void shouldSetFirstUploadedPhotoAsCover() throws Exception {
            String albumId = createAlbum(owner, "Com fotos", "PUBLIC");

            MvcResult result = mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value(containsString("/uploads/albums/")))
                    .andReturn();
            String photoUrl = objectMapper.readTree(result.getResponse().getContentAsString()).get("url").asText();

            mockMvc.perform(get("/api/albums/" + albumId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(jsonPath("$.coverPhotoUrl").value(photoUrl))
                    .andExpect(jsonPath("$.photoCount").value(1))
                    .andExpect(jsonPath("$.photos", hasSize(1)));
        }

        @Test
        @DisplayName("400 — arquivo que não é imagem é rejeitado")
        void shouldRejectInvalidFormat() throws Exception {
            String albumId = createAlbum(owner, "Com fotos", "PUBLIC");
            MockMultipartFile notImage = new MockMultipartFile(
                    "file", "notes.txt", "text/plain", "isto nao eh uma imagem".getBytes());

            mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                            .file(notImage)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PATCH edita a legenda da foto")
        void shouldUpdateCaption() throws Exception {
            String albumId = createAlbum(owner, "Com fotos", "PUBLIC");
            MvcResult result = mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(owner)))
                    .andReturn();
            String photoId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

            mockMvc.perform(patch("/api/albums/" + albumId + "/photos/" + photoId)
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"caption": "Pôr do sol"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.caption").value("Pôr do sol"));
        }

        @Test
        @DisplayName("PUT /cover troca a capa para outra foto do álbum")
        void shouldChangeCover() throws Exception {
            String albumId = createAlbum(owner, "Com fotos", "PUBLIC");
            mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                    .file(validPngUpload())
                    .header("Authorization", authHeader(owner)));
            MvcResult second = mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(owner)))
                    .andReturn();
            String secondPhotoId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText();
            String secondPhotoUrl = objectMapper.readTree(second.getResponse().getContentAsString()).get("url").asText();

            mockMvc.perform(put("/api/albums/" + albumId + "/cover")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"photoId": "%s"}
                                    """.formatted(secondPhotoId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coverPhotoUrl").value(secondPhotoUrl));
        }

        @Test
        @DisplayName("DELETE remove a foto e limpa a capa se ela era a foto removida")
        void shouldDeletePhotoAndClearCoverIfNeeded() throws Exception {
            String albumId = createAlbum(owner, "Com fotos", "PUBLIC");
            MvcResult result = mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(owner)))
                    .andReturn();
            String photoId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

            mockMvc.perform(delete("/api/albums/" + albumId + "/photos/" + photoId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/albums/" + albumId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(jsonPath("$.coverPhotoUrl").isEmpty())
                    .andExpect(jsonPath("$.photoCount").value(0));
        }

        @Test
        @DisplayName("403 — outro usuário não pode enviar foto para o álbum")
        void shouldRejectUploadByNonOwner() throws Exception {
            String albumId = createAlbum(owner, "Com fotos", "PUBLIC");
            AuthResponse stranger = registerUniqueUser();

            mockMvc.perform(multipart("/api/albums/" + albumId + "/photos")
                            .file(validPngUpload())
                            .header("Authorization", authHeader(stranger)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("photosCount do overview de perfil reflete o total de fotos em todos os álbuns")
        void shouldReflectPhotosCountInProfileOverview() throws Exception {
            String album1 = createAlbum(owner, "Álbum 1", "PUBLIC");
            String album2 = createAlbum(owner, "Álbum 2", "PUBLIC");
            mockMvc.perform(multipart("/api/albums/" + album1 + "/photos")
                    .file(validPngUpload())
                    .header("Authorization", authHeader(owner)));
            mockMvc.perform(multipart("/api/albums/" + album2 + "/photos")
                    .file(validPngUpload())
                    .header("Authorization", authHeader(owner)));

            mockMvc.perform(get("/api/profile/overview")
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.shortcuts.photosCount").value(2));
        }
    }
}
