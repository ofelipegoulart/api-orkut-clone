package com.orkutclone.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.orkutclone.api.dto.AuthResponse;
import com.orkutclone.api.model.Poll;
import com.orkutclone.api.repository.PollRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PollIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PollRepository pollRepository;

    private AuthResponse owner;

    @BeforeEach
    void setUp() throws Exception {
        owner = registerUniqueUser();
    }

    private String createCommunity(AuthResponse creator, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/community")
                        .header("Authorization", authHeader(creator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "category": "GAMES", "type": "PUBLIC", "contentPrivacy": "OPEN_TO_NON_MEMBERS"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createPoll(AuthResponse user, String communityId, String question) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                        .header("Authorization", authHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question": "%s", "options": ["Sim", "Não"]}
                                """.formatted(question)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void vote(AuthResponse user, String communityId, String pollId, String... optionIds) throws Exception {
        String idsJson = String.join(",", java.util.Arrays.stream(optionIds).map(id -> "\"" + id + "\"").toList());
        mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                .header("Authorization", authHeader(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"optionIds\": [" + idsJson + "]}"));
    }

    @Nested
    @DisplayName("POST/GET/DELETE /api/community/{id}/polls — CRUD da enquete")
    class PollCrud {

        @Test
        @DisplayName("Cria uma enquete com opções")
        void shouldCreatePoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");

            mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Qual o melhor jogo?", "options": ["Xadrez", "Damas", "Dominó"]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.question").value("Qual o melhor jogo?"))
                    .andExpect(jsonPath("$.options", hasSize(3)))
                    .andExpect(jsonPath("$.totalVotes").value(0))
                    .andExpect(jsonPath("$.multipleChoice").value(false))
                    .andExpect(jsonPath("$.viewerVoteOptionIds", hasSize(0)))
                    .andExpect(jsonPath("$.creatorId").value(owner.userId().toString()));
        }

        @Test
        @DisplayName("400 — menos de duas opções é rejeitado")
        void shouldRejectSingleOption() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");

            mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Pergunta?", "options": ["Única"]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 — pergunta em branco é rejeitada")
        void shouldRejectBlankQuestion() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");

            mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "", "options": ["Sim", "Não"]}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Lista enquetes de uma comunidade")
        void shouldListPolls() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            createPoll(owner, communityId, "Enquete 1");
            createPoll(owner, communityId, "Enquete 2");

            mockMvc.perform(get("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.results", hasSize(2)))
                    .andExpect(jsonPath("$.results[0].question").value("Enquete 2"));
        }

        @Test
        @DisplayName("404 — enquete inexistente")
        void shouldReturnNotFoundForMissingPoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");

            mockMvc.perform(get("/api/community/" + communityId + "/polls/" + java.util.UUID.randomUUID())
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE pelo dono da comunidade remove a enquete")
        void shouldDeletePollAsOwner() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            String pollId = createPoll(owner, communityId, "Para apagar");

            mockMvc.perform(delete("/api/community/" + communityId + "/polls/" + pollId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/community/" + communityId + "/polls/" + pollId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 — quem não é dono da comunidade não pode apagar a enquete")
        void shouldRejectDeleteByNonOwner() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            String pollId = createPoll(owner, communityId, "Protegida");
            AuthResponse stranger = registerUniqueUser();

            mockMvc.perform(delete("/api/community/" + communityId + "/polls/" + pollId)
                            .header("Authorization", authHeader(stranger)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /vote — votação")
    class Voting {

        @Test
        @DisplayName("Vota em uma opção e reflete no detalhe")
        void shouldVote() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            MvcResult created = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Vamos votar?", "options": ["Sim", "Não"]}
                                    """))
                    .andReturn();
            JsonNode pollNode = objectMapper.readTree(created.getResponse().getContentAsString());
            String pollId = pollNode.get("id").asText();
            String optionId = pollNode.get("options").get(0).get("id").asText();

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"optionIds": ["%s"]}
                                    """.formatted(optionId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalVotes").value(1))
                    .andExpect(jsonPath("$.viewerVoteOptionIds", contains(optionId)))
                    .andExpect(jsonPath("$.options[0].voteCount").value(1));
        }

        @Test
        @DisplayName("409 — não pode votar duas vezes na mesma enquete")
        void shouldRejectDoubleVote() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            MvcResult created = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Vamos votar?", "options": ["Sim", "Não"]}
                                    """))
                    .andReturn();
            JsonNode pollNode = objectMapper.readTree(created.getResponse().getContentAsString());
            String pollId = pollNode.get("id").asText();
            String optionId = pollNode.get("options").get(0).get("id").asText();

            vote(owner, communityId, pollId, optionId);

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"optionIds": ["%s"]}
                                    """.formatted(optionId)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("404 — vota em opção que não pertence à enquete")
        void shouldRejectVoteForUnknownOption() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            String pollId = createPoll(owner, communityId, "Enquete");

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"optionIds": ["%s"]}
                                    """.formatted(java.util.UUID.randomUUID())))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400 — enviar mais de uma opção em enquete de escolha única")
        void shouldRejectMultipleOptionsOnSingleChoicePoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            MvcResult created = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Enquete?", "options": ["Sim", "Não"], "multipleChoice": false}
                                    """))
                    .andReturn();
            JsonNode pollNode = objectMapper.readTree(created.getResponse().getContentAsString());
            String pollId = pollNode.get("id").asText();
            String option1 = pollNode.get("options").get(0).get("id").asText();
            String option2 = pollNode.get("options").get(1).get("id").asText();

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"optionIds": ["%s", "%s"]}
                                    """.formatted(option1, option2)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Enquete de múltipla escolha aceita mais de uma opção em um único voto")
        void shouldAllowMultipleOptionsOnMultipleChoicePoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            MvcResult created = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Quais você joga?", "options": ["Xadrez", "Damas", "Dominó"], "multipleChoice": true}
                                    """))
                    .andReturn();
            JsonNode pollNode = objectMapper.readTree(created.getResponse().getContentAsString());
            String pollId = pollNode.get("id").asText();
            String option1 = pollNode.get("options").get(0).get("id").asText();
            String option2 = pollNode.get("options").get(1).get("id").asText();

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"optionIds": ["%s", "%s"]}
                                    """.formatted(option1, option2)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalVotes").value(2))
                    .andExpect(jsonPath("$.viewerVoteOptionIds", containsInAnyOrder(option1, option2)));

            // voting again (even with a different option) is still rejected — one vote per user
            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"optionIds": ["%s"]}
                                    """.formatted(pollNode.get("options").get(2).get("id").asText())))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("POST /comments — comentários")
    class Comments {

        @Test
        @DisplayName("Comenta em uma enquete e aparece no detalhe")
        void shouldCommentOnPoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            String pollId = createPoll(owner, communityId, "Enquete comentada");

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/comments")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message": "Ótima enquete!"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Ótima enquete!"))
                    .andExpect(jsonPath("$.authorId").value(owner.userId().toString()));

            mockMvc.perform(get("/api/community/" + communityId + "/polls/" + pollId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(jsonPath("$.comments", hasSize(1)))
                    .andExpect(jsonPath("$.comments[0].message").value("Ótima enquete!"));
        }

        @Test
        @DisplayName("400 — comentário em branco é rejeitado")
        void shouldRejectBlankComment() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            String pollId = createPoll(owner, communityId, "Enquete");

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/comments")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message": ""}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Dashboard — widget de enquete ativa")
    class DashboardActivePoll {

        @Test
        @DisplayName("dashboard.activePoll reflete a enquete mais recente da comunidade")
        void shouldPopulateActivePollOnDashboard() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            createPoll(owner, communityId, "Enquete antiga");
            String latestPollId = createPoll(owner, communityId, "Enquete recente");

            mockMvc.perform(get("/api/community/" + communityId + "/dashboard")
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activePoll.id").value(latestPollId))
                    .andExpect(jsonPath("$.activePoll.question").value("Enquete recente"))
                    .andExpect(jsonPath("$.activePoll.voteOptions", hasSize(2)));
        }

        @Test
        @DisplayName("dashboard.activePoll é nulo quando a comunidade não tem enquetes")
        void shouldReturnNullActivePollWhenNoPolls() throws Exception {
            String communityId = createCommunity(owner, "Comunidade sem enquetes");

            mockMvc.perform(get("/api/community/" + communityId + "/dashboard")
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activePoll").isEmpty());
        }
    }

    @Nested
    @DisplayName("Descrição e imagem da enquete")
    class DescriptionAndImage {

        @Test
        @DisplayName("Cria enquete com descrição e imageUrl e reflete no detalhe")
        void shouldCreatePollWithDescriptionAndImage() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");

            mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Qual o melhor jogo?", "description": "Vote no seu favorito", "imageUrl": "/uploads/avatars/poll.png", "options": ["Xadrez", "Damas"]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.description").value("Vote no seu favorito"))
                    .andExpect(jsonPath("$.imageUrl").value("/uploads/avatars/poll.png"));
        }

        @Test
        @DisplayName("Upload de imagem retorna URL pública para usar na criação")
        void shouldUploadPollImage() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            MockMultipartFile file = new MockMultipartFile("file", "poll.png", "image/png", baos.toByteArray());

            mockMvc.perform(multipart("/api/community/" + communityId + "/polls/image")
                            .file(file)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url", containsString("/uploads/avatars/")));
        }
    }

    @Nested
    @DisplayName("Data de fechamento")
    class ClosingDate {

        @Test
        @DisplayName("400 — data de fechamento no passado é rejeitada")
        void shouldRejectPastClosingDate() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");

            mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Enquete?", "options": ["Sim", "Não"], "closesAt": "2020-01-01T00:00:00Z"}
                                    """))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Enquete com data de fechamento futura aparece aberta no detalhe")
        void shouldExposeFutureClosingDateAsOpen() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");

            mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Enquete?", "options": ["Sim", "Não"], "closesAt": "2099-01-01T00:00:00Z"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.closesAt").isNotEmpty())
                    .andExpect(jsonPath("$.closed").value(false));
        }

        @Test
        @DisplayName("409 — não pode votar em enquete fechada")
        void shouldRejectVoteOnClosedPoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            MvcResult created = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Enquete?", "options": ["Sim", "Não"]}
                                    """))
                    .andReturn();
            JsonNode pollNode = objectMapper.readTree(created.getResponse().getContentAsString());
            String pollId = pollNode.get("id").asText();
            String optionId = pollNode.get("options").get(0).get("id").asText();

            Poll poll = pollRepository.findById(java.util.UUID.fromString(pollId)).orElseThrow();
            poll.setClosesAt(Instant.now().minusSeconds(60));
            pollRepository.save(poll);

            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/vote")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"optionIds": ["%s"]}
                                    """.formatted(optionId)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Visibilidade do voto e tag no comentário")
    class VoteVisibility {

        @Test
        @DisplayName("anonymous=false expõe votedOptionIds no comentário de quem votou")
        void shouldExposeVotedOptionOnPublicPoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            MvcResult created = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Enquete?", "options": ["Sim", "Não"], "anonymous": false}
                                    """))
                    .andReturn();
            JsonNode pollNode = objectMapper.readTree(created.getResponse().getContentAsString());
            String pollId = pollNode.get("id").asText();
            String optionId = pollNode.get("options").get(0).get("id").asText();

            vote(owner, communityId, pollId, optionId);
            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/comments")
                    .header("Authorization", authHeader(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"message": "Meu comentário"}
                            """));

            mockMvc.perform(get("/api/community/" + communityId + "/polls/" + pollId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(jsonPath("$.comments[0].votedOptionIds", contains(optionId)));
        }

        @Test
        @DisplayName("anonymous=true esconde votedOptionIds no comentário")
        void shouldHideVotedOptionOnAnonymousPoll() throws Exception {
            String communityId = createCommunity(owner, "Comunidade dos Jogos");
            MvcResult created = mockMvc.perform(post("/api/community/" + communityId + "/polls")
                            .header("Authorization", authHeader(owner))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"question": "Enquete?", "options": ["Sim", "Não"], "anonymous": true}
                                    """))
                    .andReturn();
            JsonNode pollNode = objectMapper.readTree(created.getResponse().getContentAsString());
            String pollId = pollNode.get("id").asText();
            String optionId = pollNode.get("options").get(0).get("id").asText();

            vote(owner, communityId, pollId, optionId);
            mockMvc.perform(post("/api/community/" + communityId + "/polls/" + pollId + "/comments")
                    .header("Authorization", authHeader(owner))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"message": "Meu comentário"}
                            """));

            mockMvc.perform(get("/api/community/" + communityId + "/polls/" + pollId)
                            .header("Authorization", authHeader(owner)))
                    .andExpect(jsonPath("$.comments[0].votedOptionIds", hasSize(0)));
        }
    }
}
