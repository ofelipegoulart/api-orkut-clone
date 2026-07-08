package com.orkutclone.api.service;

import com.orkutclone.api.dto.CreateScrapRequest;
import com.orkutclone.api.dto.ScrapResponse;
import com.orkutclone.api.dto.UpdateScrapRequest;
import com.orkutclone.api.model.Scrap;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.Role;
import com.orkutclone.api.repository.ScrapRepository;
import com.orkutclone.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ScrapService scrapService;

    @Mock
    private ProfileStatisticsService profileStatisticsService;

    private User felipe;
    private User maria;
    private User joao;

    @BeforeEach
    void setUp() {
        felipe = User.builder()
                .id(UUID.randomUUID())
                .name("Felipe Goulart")
                .email("felipe@orkut.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        maria = User.builder()
                .id(UUID.randomUUID())
                .name("Maria Silva")
                .email("maria@orkut.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        joao = User.builder()
                .id(UUID.randomUUID())
                .name("João Santos")
                .email("joao@orkut.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        setAuthenticatedUser(felipe);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setAuthenticatedUser(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    private Scrap buildScrap(User author, User owner, String content, boolean isPrivate) {
        return Scrap.builder()
                .id(UUID.randomUUID())
                .content(content)
                .author(author)
                .owner(owner)
                .isPrivate(isPrivate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void stubSaveScrap() {
        when(scrapRepository.save(any(Scrap.class))).thenAnswer(inv -> {
            Scrap s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            if (s.getCreatedAt() == null) s.setCreatedAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            return s;
        });
    }

    @Nested
    @DisplayName("Criar Recado - Deixando um recado no mural de alguém")
    class CreateScrap {

        @Test
        @DisplayName("Felipe deixa um recado público no mural da Maria")
        void shouldCreatePublicScrapOnSomeoneWall() {
            CreateScrapRequest request = new CreateScrapRequest(
                    "Oi Maria! Saudades das festas juninas!", maria.getId(), false, null
            );
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).isEqualTo("Oi Maria! Saudades das festas juninas!");
            assertThat(response.authorId()).isEqualTo(felipe.getId());
            assertThat(response.authorName()).isEqualTo("Felipe Goulart");
            assertThat(response.ownerId()).isEqualTo(maria.getId());
            assertThat(response.ownerName()).isEqualTo("Maria Silva");
            assertThat(response.isPrivate()).isFalse();
        }

        @Test
        @DisplayName("Felipe deixa um recado privado no mural da Maria")
        void shouldCreatePrivateScrap() {
            CreateScrapRequest request = new CreateScrapRequest(
                    "Maria, assunto particular...", maria.getId(), true, null
            );
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.isPrivate()).isTrue();
        }

        @Test
        @DisplayName("Não pode enviar recado para si mesmo")
        void shouldRejectScrapToSelf() {
            CreateScrapRequest request = new CreateScrapRequest(
                    "Pensamento do dia: o Orkut era melhor!", felipe.getId(), false, null
            );
            when(userRepository.findById(felipe.getId())).thenReturn(Optional.of(felipe));

            assertThatThrownBy(() -> scrapService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Cannot send a scrap to yourself");
        }

        @Test
        @DisplayName("Não deve criar recado para usuário inexistente")
        void shouldRejectScrapToNonExistentUser() {
            UUID fakeUserId = UUID.randomUUID();
            CreateScrapRequest request = new CreateScrapRequest("Oi!", fakeUserId, false, null);
            when(userRepository.findById(fakeUserId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("isPrivate null deve ser tratado como false")
        void shouldTreatNullIsPrivateAsFalse() {
            CreateScrapRequest request = new CreateScrapRequest(
                    "Recado sem privacidade definida", maria.getId(), null, null
            );
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.isPrivate()).isFalse();
        }
    }

    @Nested
    @DisplayName("Sanitização HTML e Limite de Tamanho")
    class HtmlSanitizationAndLimits {

        @Test
        @DisplayName("Deve permitir HTML básico seguro (negrito, itálico, links)")
        void shouldAllowSafeBasicHtml() {
            String htmlContent = "<b>Oi Maria!</b> Visita meu perfil <a href=\"https://orkut.com\">aqui</a>";
            CreateScrapRequest request = new CreateScrapRequest(htmlContent, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("<b>Oi Maria!</b>");
            assertThat(response.content()).contains("<a");
        }

        @Test
        @DisplayName("Deve remover tags <script> - proteção contra XSS")
        void shouldStripScriptTags() {
            String xssAttempt = "Oi!<script>alert('hackeado')</script> Tudo bem?";
            CreateScrapRequest request = new CreateScrapRequest(xssAttempt, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("<script>");
            assertThat(response.content()).doesNotContain("alert");
            assertThat(response.content()).contains("Oi!");
            assertThat(response.content()).contains("Tudo bem?");
        }

        @Test
        @DisplayName("Deve remover event handlers tipo onclick - proteção XSS")
        void shouldStripEventHandlers() {
            String xssAttempt = "<div onclick=\"alert('xss')\">Clique aqui</div>";
            CreateScrapRequest request = new CreateScrapRequest(xssAttempt, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("onclick");
            assertThat(response.content()).contains("Clique aqui");
        }

        @Test
        @DisplayName("Deve remover javascript: em href de links")
        void shouldStripJavascriptProtocol() {
            String xssAttempt = "<a href=\"javascript:alert('xss')\">Link malicioso</a>";
            CreateScrapRequest request = new CreateScrapRequest(xssAttempt, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("javascript:");
        }

        @Test
        @DisplayName("Deve remover tag <iframe> - proteção contra embed malicioso")
        void shouldStripIframeTags() {
            String iframeAttempt = "<iframe src=\"https://malicious.com\"></iframe>Recado normal";
            CreateScrapRequest request = new CreateScrapRequest(iframeAttempt, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("<iframe");
            assertThat(response.content()).contains("Recado normal");
        }

        @Test
        @DisplayName("Deve remover <form> e <input> - proteção contra phishing")
        void shouldStripFormTags() {
            String phishing = "<form action=\"https://evil.com\"><input type=\"text\" name=\"senha\">Coloque sua senha</form>";
            CreateScrapRequest request = new CreateScrapRequest(phishing, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("<form");
            assertThat(response.content()).doesNotContain("<input");
        }

        @Test
        @DisplayName("Deve permitir <img> com src válido")
        void shouldAllowImgWithValidSrc() {
            String imgContent = "<img src=\"https://i.imgur.com/foto.png\" alt=\"foto\">";
            CreateScrapRequest request = new CreateScrapRequest(imgContent, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("<img");
            assertThat(response.content()).contains("src=");
        }

        @Test
        @DisplayName("Deve permitir tags de formatação: h1, h2, h3, span, div")
        void shouldAllowFormattingTags() {
            String formatted = "<h1>Título</h1><h2>Sub</h2><span class=\"destaque\">texto</span><div style=\"color:red\">vermelho</div>";
            CreateScrapRequest request = new CreateScrapRequest(formatted, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("<h1>");
            assertThat(response.content()).contains("<span");
            assertThat(response.content()).contains("<div");
        }

        @Test
        @DisplayName("Deve rejeitar recado com mais de 1024 caracteres")
        void shouldRejectContentExceedingMaxLength() {
            String longContent = "a".repeat(1025);
            CreateScrapRequest request = new CreateScrapRequest(longContent, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));

            assertThatThrownBy(() -> scrapService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("exceeds maximum length of 1024");
        }

        @Test
        @DisplayName("Deve aceitar recado com exatamente 1024 caracteres")
        void shouldAcceptContentAtMaxLength() {
            String maxContent = "a".repeat(1024);
            CreateScrapRequest request = new CreateScrapRequest(maxContent, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).hasSize(1024);
        }

        @Test
        @DisplayName("Deve sanitizar antes de verificar o tamanho (tags removidas reduzem length)")
        void shouldSanitizeBeforeCheckingLength() {
            String scriptTag = "<script>" + "x".repeat(500) + "</script>";
            String safeContent = "a".repeat(800);
            String combined = safeContent + scriptTag;
            assertThat(combined.length()).isGreaterThan(1024);

            CreateScrapRequest request = new CreateScrapRequest(combined, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("<script>");
            assertThat(response.content().length()).isLessThanOrEqualTo(1024);
        }

        @Test
        @DisplayName("Deve sanitizar HTML no update também, não só na criação")
        void shouldSanitizeOnUpdate() {
            Scrap scrap = buildScrap(felipe, maria, "Texto original", false);
            UpdateScrapRequest request = new UpdateScrapRequest(
                    "Texto editado <script>alert('xss')</script>"
            );
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));
            when(scrapRepository.save(any(Scrap.class))).thenAnswer(inv -> inv.getArgument(0));

            ScrapResponse response = scrapService.update(scrap.getId(), request);

            assertThat(response.content()).doesNotContain("<script>");
            assertThat(response.content()).contains("Texto editado");
        }

        @Test
        @DisplayName("Deve rejeitar update que excede 1024 caracteres")
        void shouldRejectUpdateExceedingMaxLength() {
            Scrap scrap = buildScrap(felipe, maria, "Texto original", false);
            UpdateScrapRequest request = new UpdateScrapRequest("b".repeat(1025));
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            assertThatThrownBy(() -> scrapService.update(scrap.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("exceeds maximum length of 1024");
        }

        @Test
        @DisplayName("Deve remover atributo style com javascript: via safelist restrita")
        void shouldStripStyleAttributeToPreventCssXss() {
            String cssXss = "<div style=\"background:url(javascript:alert('xss'))\">Texto</div>";
            CreateScrapRequest request = new CreateScrapRequest(cssXss, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("Texto");
        }

        @Test
        @DisplayName("Deve remover tag <svg> com onload - vetor XSS comum")
        void shouldStripSvgOnload() {
            String svgXss = "<svg onload=\"alert('xss')\">teste</svg>";
            CreateScrapRequest request = new CreateScrapRequest(svgXss, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("onload");
            assertThat(response.content()).doesNotContain("<svg");
        }

        @Test
        @DisplayName("Deve lidar com entidades HTML encoded para XSS")
        void shouldHandleEncodedXss() {
            String encodedXss = "<img src=x onerror=\"alert('xss')\">";
            CreateScrapRequest request = new CreateScrapRequest(encodedXss, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).doesNotContain("onerror");
        }

        @Test
        @DisplayName("Conteúdo que fica vazio após sanitização deve ser rejeitado (só tags maliciosas)")
        void shouldRejectContentThatBecomesEmptyAfterSanitization() {
            String onlyScript = "<script>alert('xss')</script>";
            CreateScrapRequest request = new CreateScrapRequest(onlyScript, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));

            assertThatThrownBy(() -> scrapService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap content cannot be empty");
        }

        @Test
        @DisplayName("Conteúdo que fica só com espaços após sanitização deve ser rejeitado")
        void shouldRejectContentThatBecomesBlankAfterSanitization() {
            String scriptWithSpaces = "   <script>alert('xss')</script>   ";
            CreateScrapRequest request = new CreateScrapRequest(scriptWithSpaces, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));

            assertThatThrownBy(() -> scrapService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap content cannot be empty");
        }

        @Test
        @DisplayName("Update com conteúdo que fica vazio após sanitização deve ser rejeitado")
        void shouldRejectUpdateThatBecomesEmptyAfterSanitization() {
            Scrap scrap = buildScrap(felipe, maria, "Texto original", false);
            UpdateScrapRequest request = new UpdateScrapRequest("<script>hack</script>");
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            assertThatThrownBy(() -> scrapService.update(scrap.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap content cannot be empty");
        }

        @Test
        @DisplayName("Texto puro sem HTML deve funcionar normalmente")
        void shouldHandlePlainText() {
            CreateScrapRequest request = new CreateScrapRequest(
                    "Apenas um recado normal sem HTML nenhum :)", maria.getId(), false, null
            );
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).isEqualTo("Apenas um recado normal sem HTML nenhum :)");
        }

        @Test
        @DisplayName("Emojis e caracteres especiais devem ser preservados")
        void shouldPreserveEmojisAndSpecialChars() {
            CreateScrapRequest request = new CreateScrapRequest(
                    "Oi Maria! ❤️🎉 Café & companhia à tarde? Ação! ñ", maria.getId(), false, null
            );
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("❤️");
            assertThat(response.content()).contains("🎉");
            assertThat(response.content()).contains("ñ");
        }
    }

    @Nested
    @DisplayName("authorAvatar - Foto do autor no recado")
    class AuthorAvatar {

        @Test
        @DisplayName("authorAvatar deve retornar a profilePicture do autor")
        void shouldReturnAuthorProfilePicture() {
            felipe.setProfilePicture("data:image/png;base64,foto123");
            CreateScrapRequest request = new CreateScrapRequest("Teste", maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.authorAvatar()).isEqualTo("data:image/png;base64,foto123");
        }

        @Test
        @DisplayName("authorAvatar deve ser null quando autor não tem foto")
        void shouldReturnNullAvatarWhenNoProfilePicture() {
            felipe.setProfilePicture(null);
            CreateScrapRequest request = new CreateScrapRequest("Teste", maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.authorAvatar()).isNull();
        }
    }

    @Nested
    @DisplayName("Ver Recado - Abrindo um recado específico")
    class FindById {

        @Test
        @DisplayName("Qualquer um pode ver um recado público")
        void shouldAllowViewingPublicScrap() {
            Scrap publicScrap = buildScrap(maria, joao, "Oi João!", false);
            when(scrapRepository.findById(publicScrap.getId())).thenReturn(Optional.of(publicScrap));

            ScrapResponse response = scrapService.findById(publicScrap.getId());

            assertThat(response.content()).isEqualTo("Oi João!");
        }

        @Test
        @DisplayName("O autor pode ver seu próprio recado privado")
        void authorCanViewOwnPrivateScrap() {
            Scrap privateScrap = buildScrap(felipe, maria, "Mensagem privada", true);
            when(scrapRepository.findById(privateScrap.getId())).thenReturn(Optional.of(privateScrap));

            ScrapResponse response = scrapService.findById(privateScrap.getId());

            assertThat(response.content()).isEqualTo("Mensagem privada");
        }

        @Test
        @DisplayName("O dono do mural pode ver recados privados em seu mural")
        void wallOwnerCanViewPrivateScrapOnTheirWall() {
            Scrap privateScrap = buildScrap(maria, felipe, "Mensagem privada pra Felipe", true);
            when(scrapRepository.findById(privateScrap.getId())).thenReturn(Optional.of(privateScrap));

            ScrapResponse response = scrapService.findById(privateScrap.getId());

            assertThat(response.content()).isEqualTo("Mensagem privada pra Felipe");
        }

        @Test
        @DisplayName("Terceiro NÃO pode ver recado privado entre outras pessoas")
        void thirdPartyShouldNotSeePrivateScrap() {
            Scrap privateScrap = buildScrap(maria, joao, "Segredo entre Maria e João", true);
            when(scrapRepository.findById(privateScrap.getId())).thenReturn(Optional.of(privateScrap));

            assertThatThrownBy(() -> scrapService.findById(privateScrap.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("This scrap is private");
        }

        @Test
        @DisplayName("Deve retornar 404 para recado inexistente")
        void shouldReturn404ForNonExistentScrap() {
            UUID fakeId = UUID.randomUUID();
            when(scrapRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.findById(fakeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Recado público no mural de um terceiro - Felipe pode ver")
        void felipeCanViewPublicScrapOnThirdPartyWall() {
            Scrap publicScrap = buildScrap(maria, joao, "Oi João!", false);
            when(scrapRepository.findById(publicScrap.getId())).thenReturn(Optional.of(publicScrap));

            ScrapResponse response = scrapService.findById(publicScrap.getId());

            assertThat(response.content()).isEqualTo("Oi João!");
            assertThat(response.authorName()).isEqualTo("Maria Silva");
            assertThat(response.ownerName()).isEqualTo("João Santos");
        }

        @Test
        @DisplayName("Deve retornar null para createdAt e updatedAt quando scrap não tem timestamps")
        void shouldHandleNullTimestamps() {
            Scrap scrap = Scrap.builder()
                    .id(UUID.randomUUID())
                    .content("Sem timestamps")
                    .author(maria)
                    .owner(felipe)
                    .isPrivate(false)
                    .createdAt(null)
                    .updatedAt(null)
                    .build();
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            ScrapResponse response = scrapService.findById(scrap.getId());

            assertThat(response.createdAt()).isNull();
            assertThat(response.updatedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("Editar Recado - Corrigindo o que eu escrevi")
    class UpdateScrap {

        @Test
        @DisplayName("O autor pode editar o conteúdo do seu recado")
        void authorCanEditScrap() {
            Scrap scrap = buildScrap(felipe, maria, "Texto com erro", false);
            UpdateScrapRequest request = new UpdateScrapRequest("Texto corrigido!");
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));
            when(scrapRepository.save(any(Scrap.class))).thenAnswer(inv -> inv.getArgument(0));

            ScrapResponse response = scrapService.update(scrap.getId(), request);

            assertThat(response.content()).isEqualTo("Texto corrigido!");
        }

        @Test
        @DisplayName("O dono do mural NÃO pode editar recados de outros")
        void wallOwnerCannotEditOthersScrap() {
            Scrap scrap = buildScrap(maria, felipe, "Recado da Maria", false);
            UpdateScrapRequest request = new UpdateScrapRequest("Tentando editar");
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            assertThatThrownBy(() -> scrapService.update(scrap.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only the author can edit");
        }

        @Test
        @DisplayName("Um terceiro NÃO pode editar recados alheios")
        void thirdPartyCannotEditScrap() {
            Scrap scrap = buildScrap(maria, joao, "Recado entre Maria e João", false);
            UpdateScrapRequest request = new UpdateScrapRequest("Hackeando...");
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            assertThatThrownBy(() -> scrapService.update(scrap.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only the author can edit");
        }

        @Test
        @DisplayName("Editar recado inexistente retorna 404")
        void shouldReturn404WhenEditingNonExistentScrap() {
            UUID fakeId = UUID.randomUUID();
            UpdateScrapRequest request = new UpdateScrapRequest("Tentando");
            when(scrapRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.update(fakeId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Autor pode editar recado privado que ele mesmo criou")
        void authorCanEditOwnPrivateScrap() {
            Scrap scrap = buildScrap(felipe, maria, "Privado original", true);
            UpdateScrapRequest request = new UpdateScrapRequest("Privado editado");
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));
            when(scrapRepository.save(any(Scrap.class))).thenAnswer(inv -> inv.getArgument(0));

            ScrapResponse response = scrapService.update(scrap.getId(), request);

            assertThat(response.content()).isEqualTo("Privado editado");
            assertThat(response.isPrivate()).isTrue();
        }
    }

    @Nested
    @DisplayName("Apagar Recado - Removendo recados")
    class DeleteScrap {

        @Test
        @DisplayName("O autor pode apagar seu próprio recado")
        void authorCanDeleteOwnScrap() {
            Scrap scrap = buildScrap(felipe, maria, "Vou apagar isso", false);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            scrapService.delete(scrap.getId());

            verify(scrapRepository).delete(scrap);
        }

        @Test
        @DisplayName("O dono do mural pode apagar recados no seu mural")
        void wallOwnerCanDeleteScrapOnTheirWall() {
            Scrap scrap = buildScrap(maria, felipe, "Recado indesejado", false);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            scrapService.delete(scrap.getId());

            verify(scrapRepository).delete(scrap);
        }

        @Test
        @DisplayName("Terceiro NÃO pode apagar recados de outros")
        void thirdPartyCannotDeleteScrap() {
            Scrap scrap = buildScrap(maria, joao, "Não é da conta do Felipe", false);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            assertThatThrownBy(() -> scrapService.delete(scrap.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only the author or wall owner can delete");

            verify(scrapRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Apagar recado inexistente retorna 404")
        void shouldReturn404WhenDeletingNonExistentScrap() {
            UUID fakeId = UUID.randomUUID();
            when(scrapRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.delete(fakeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Dono do mural pode apagar recado PRIVADO no seu mural")
        void wallOwnerCanDeletePrivateScrapOnTheirWall() {
            Scrap scrap = buildScrap(maria, felipe, "Privado indesejado", true);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            scrapService.delete(scrap.getId());

            verify(scrapRepository).delete(scrap);
        }

        @Test
        @DisplayName("Terceiro NÃO pode apagar recado privado entre outros")
        void thirdPartyCannotDeletePrivateScrap() {
            Scrap scrap = buildScrap(maria, joao, "Segredo", true);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            assertThatThrownBy(() -> scrapService.delete(scrap.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only the author or wall owner can delete");
        }

    }

    @Nested
    @DisplayName("Listar Recados do Mural - Visitando o perfil de alguém")
    class FindByOwner {

        @Test
        @DisplayName("Deve listar recados visíveis do mural de alguém")
        void shouldListVisibleScrapsOnWall() {
            Scrap scrap1 = buildScrap(felipe, maria, "Recado 1", false);
            Scrap scrap2 = buildScrap(joao, maria, "Recado 2", false);
            Page<Scrap> page = new PageImpl<>(List.of(scrap1, scrap2));

            when(userRepository.existsById(maria.getId())).thenReturn(true);
            when(scrapRepository.findVisibleByOwnerId(eq(maria.getId()), eq(felipe.getId()), any(PageRequest.class)))
                    .thenReturn(page);

            Page<ScrapResponse> result = scrapService.findByOwner(maria.getId(), 0, 10);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).ownerName()).isEqualTo("Maria Silva");
        }

        @Test
        @DisplayName("Deve retornar 404 se o dono do mural não existe")
        void shouldReturn404ForNonExistentOwner() {
            UUID fakeId = UUID.randomUUID();
            when(userRepository.existsById(fakeId)).thenReturn(false);

            assertThatThrownBy(() -> scrapService.findByOwner(fakeId, 0, 10))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Mural vazio retorna página sem conteúdo")
        void shouldReturnEmptyPageForEmptyWall() {
            Page<Scrap> emptyPage = new PageImpl<>(Collections.emptyList());
            when(userRepository.existsById(maria.getId())).thenReturn(true);
            when(scrapRepository.findVisibleByOwnerId(eq(maria.getId()), eq(felipe.getId()), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            Page<ScrapResponse> result = scrapService.findByOwner(maria.getId(), 0, 10);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Deve passar o viewerId correto para filtrar privacidade")
        void shouldPassCorrectViewerId() {
            Page<Scrap> emptyPage = new PageImpl<>(Collections.emptyList());
            when(userRepository.existsById(maria.getId())).thenReturn(true);
            when(scrapRepository.findVisibleByOwnerId(any(), any(), any())).thenReturn(emptyPage);

            scrapService.findByOwner(maria.getId(), 0, 10);

            verify(scrapRepository).findVisibleByOwnerId(
                    eq(maria.getId()),
                    eq(felipe.getId()),
                    any(PageRequest.class)
            );
        }
    }

    @Nested
    @DisplayName("Recados Enviados - Vendo o que eu mandei")
    class FindSent {

        @Test
        @DisplayName("Deve listar os recados que eu enviei")
        void shouldListSentScraps() {
            Scrap scrap1 = buildScrap(felipe, maria, "Oi Maria!", false);
            Scrap scrap2 = buildScrap(felipe, joao, "E aí João!", false);
            Page<Scrap> page = new PageImpl<>(List.of(scrap1, scrap2));

            when(scrapRepository.findByAuthorIdOrderByCreatedAtDesc(eq(felipe.getId()), any(PageRequest.class)))
                    .thenReturn(page);

            Page<ScrapResponse> result = scrapService.findSent(0, 10);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent()).allSatisfy(scrap ->
                    assertThat(scrap.authorId()).isEqualTo(felipe.getId())
            );
        }

        @Test
        @DisplayName("Lista vazia quando não enviei nenhum recado")
        void shouldReturnEmptyWhenNoSentScraps() {
            Page<Scrap> emptyPage = new PageImpl<>(Collections.emptyList());
            when(scrapRepository.findByAuthorIdOrderByCreatedAtDesc(eq(felipe.getId()), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            Page<ScrapResponse> result = scrapService.findSent(0, 10);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cenários de troca de contexto - Vários usuários interagindo")
    class MultiUserScenarios {

        @Test
        @DisplayName("Maria logada cria recado no mural do João, Felipe não pode editar")
        void mariaCreatesScrapFelipeCannotEdit() {
            setAuthenticatedUser(maria);
            Scrap scrap = buildScrap(maria, joao, "Recado da Maria pro João", false);

            setAuthenticatedUser(felipe);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            assertThatThrownBy(() -> scrapService.update(scrap.getId(), new UpdateScrapRequest("Hack")))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only the author can edit");
        }

        @Test
        @DisplayName("João pode apagar recado que a Maria deixou no mural dele")
        void joaoCanDeleteScrapOnHisWall() {
            Scrap scrap = buildScrap(maria, joao, "Recado indesejado", false);

            setAuthenticatedUser(joao);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            scrapService.delete(scrap.getId());

            verify(scrapRepository).delete(scrap);
        }

        @Test
        @DisplayName("Maria pode ver recado privado que Felipe deixou no mural dela")
        void mariaCanSeePrivateScrapOnHerWall() {
            Scrap scrap = buildScrap(felipe, maria, "Segredo pro mural da Maria", true);

            setAuthenticatedUser(maria);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            ScrapResponse response = scrapService.findById(scrap.getId());

            assertThat(response.content()).isEqualTo("Segredo pro mural da Maria");
        }
    }

    @Nested
    @DisplayName("Concorrência - Operações simultâneas no mesmo scrap")
    class Concurrency {

        @Test
        @DisplayName("Segundo delete falha com 404 quando outro usuário já deletou o scrap")
        void shouldReturn404WhenScrapAlreadyDeletedByConcurrentRequest() {
            Scrap scrap = buildScrap(felipe, maria, "Recado", false);

            when(scrapRepository.findById(scrap.getId()))
                    .thenReturn(Optional.of(scrap))
                    .thenReturn(Optional.empty());

            scrapService.delete(scrap.getId());
            verify(scrapRepository).delete(scrap);

            assertThatThrownBy(() -> scrapService.delete(scrap.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Autor e dono do mural deletam ao mesmo tempo — segundo recebe 404")
        void shouldHandle404WhenBothAuthorAndOwnerTryToDelete() {
            Scrap scrap = buildScrap(felipe, maria, "Recado", false);

            when(scrapRepository.findById(scrap.getId()))
                    .thenReturn(Optional.of(scrap))
                    .thenReturn(Optional.empty());

            scrapService.delete(scrap.getId());

            setAuthenticatedUser(maria);
            assertThatThrownBy(() -> scrapService.delete(scrap.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Update de scrap que foi deletado por outra thread retorna 404")
        void shouldReturn404WhenUpdatingDeletedScrap() {
            UUID scrapId = UUID.randomUUID();
            when(scrapRepository.findById(scrapId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.update(scrapId, new UpdateScrapRequest("Novo texto")))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Leitura de scrap que foi deletado entre a listagem e o clique retorna 404")
        void shouldReturn404WhenReadingJustDeletedScrap() {
            UUID scrapId = UUID.randomUUID();
            when(scrapRepository.findById(scrapId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.findById(scrapId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }
    }

    @Nested
    @DisplayName("Idempotência - Mesma operação repetida")
    class Idempotency {

        @Test
        @DisplayName("Deletar scrap duas vezes — segunda vez retorna 404 graciosamente")
        void shouldHandle404OnSecondDelete() {
            Scrap scrap = buildScrap(felipe, maria, "Recado", false);

            when(scrapRepository.findById(scrap.getId()))
                    .thenReturn(Optional.of(scrap))
                    .thenReturn(Optional.empty());

            scrapService.delete(scrap.getId());

            assertThatThrownBy(() -> scrapService.delete(scrap.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Mesmo update aplicado duas vezes produz resultado idêntico")
        void shouldProduceIdenticalResultOnRepeatedUpdate() {
            Scrap scrap = buildScrap(felipe, maria, "Texto original", false);
            UpdateScrapRequest request = new UpdateScrapRequest("Texto atualizado");

            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));
            when(scrapRepository.save(any(Scrap.class))).thenAnswer(inv -> inv.getArgument(0));

            ScrapResponse first = scrapService.update(scrap.getId(), request);
            ScrapResponse second = scrapService.update(scrap.getId(), request);

            assertThat(first.content()).isEqualTo(second.content());
            assertThat(first.content()).isEqualTo("Texto atualizado");
        }

        @Test
        @DisplayName("Criar dois scraps idênticos gera entidades separadas")
        void shouldCreateSeparateEntitiesForDuplicateContent() {
            CreateScrapRequest request = new CreateScrapRequest("Oi Maria!", maria.getId(), false, null);

            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse first = scrapService.create(request);
            ScrapResponse second = scrapService.create(request);

            assertThat(first.id()).isNotEqualTo(second.id());
            assertThat(first.content()).isEqualTo(second.content());
        }
    }

    @Nested
    @DisplayName("Consistência Cross-Module - Dados refletem mudanças de outros módulos")
    class CrossModuleConsistency {

        @Test
        @DisplayName("Author muda o nome → toResponse do scrap reflete o nome novo")
        void shouldReflectAuthorNameChange() {
            Scrap scrap = buildScrap(felipe, maria, "Recado", false);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            ScrapResponse before = scrapService.findById(scrap.getId());
            assertThat(before.authorName()).isEqualTo("Felipe Goulart");

            felipe.setName("Felipe Silva");

            ScrapResponse after = scrapService.findById(scrap.getId());
            assertThat(after.authorName()).isEqualTo("Felipe Silva");
        }

        @Test
        @DisplayName("Author atualiza avatar → authorAvatar do scrap reflete")
        void shouldReflectAuthorAvatarChange() {
            Scrap scrap = buildScrap(felipe, maria, "Recado", false);
            felipe.setProfilePicture(null);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            ScrapResponse before = scrapService.findById(scrap.getId());
            assertThat(before.authorAvatar()).isNull();

            felipe.setProfilePicture("data:image/png;base64,novaFoto");

            ScrapResponse after = scrapService.findById(scrap.getId());
            assertThat(after.authorAvatar()).isEqualTo("data:image/png;base64,novaFoto");
        }

        @Test
        @DisplayName("Owner muda o nome → ownerName do scrap reflete")
        void shouldReflectOwnerNameChange() {
            Scrap scrap = buildScrap(felipe, maria, "Recado", false);
            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            ScrapResponse before = scrapService.findById(scrap.getId());
            assertThat(before.ownerName()).isEqualTo("Maria Silva");

            maria.setName("Maria Santos");

            ScrapResponse after = scrapService.findById(scrap.getId());
            assertThat(after.ownerName()).isEqualTo("Maria Santos");
        }
    }

    @Nested
    @DisplayName("Ataques Reais Simulados - Payloads maliciosos além do XSS")
    class RealAttacks {

        @Test
        @DisplayName("Payload gigante (100KB) deve ser rejeitado pelo limite de 1024")
        void shouldRejectGiantPayload() {
            String giant = "A".repeat(100_000);
            CreateScrapRequest request = new CreateScrapRequest(giant, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));

            assertThatThrownBy(() -> scrapService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("exceeds maximum length of 1024");
        }

        @Test
        @DisplayName("Unicode zero-width characters devem ser preservados (não são perigosos)")
        void shouldHandleZeroWidthCharacters() {
            String zeroWidth = "Texto​normal​com​zero​width";
            CreateScrapRequest request = new CreateScrapRequest(zeroWidth, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("​");
        }

        @Test
        @DisplayName("Right-to-left override character (U+202E) — não deve causar crash")
        void shouldHandleRtlOverrideCharacter() {
            String rtl = "Texto normal ‮oxet odatrevni";
            CreateScrapRequest request = new CreateScrapRequest(rtl, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).isNotBlank();
        }

        @Test
        @DisplayName("Null bytes (\\0) no conteúdo — não deve causar crash")
        void shouldHandleNullBytes() {
            String nullBytes = "Texto\0com\0null\0bytes";
            CreateScrapRequest request = new CreateScrapRequest(nullBytes, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).isNotBlank();
        }

        @Test
        @DisplayName("HTML profundamente aninhado — não deve causar stack overflow")
        void shouldHandleDeeplyNestedHtml() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) sb.append("<div>");
            sb.append("Texto");
            for (int i = 0; i < 10; i++) sb.append("</div>");
            CreateScrapRequest request = new CreateScrapRequest(sb.toString(), maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("Texto");
        }

        @Test
        @DisplayName("Homoglyph attack — caracteres visuais parecidos com ASCII")
        void shouldHandleHomoglyphAttack() {
            String homoglyph = "аdmіn@оrkut.cоm";
            CreateScrapRequest request = new CreateScrapRequest(
                    "Manda mensagem pra " + homoglyph, maria.getId(), false, null
            );
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains(homoglyph);
        }

        @Test
        @DisplayName("Caracteres de controle Unicode (U+0000 a U+001F) — não deve quebrar")
        void shouldHandleControlCharacters() {
            String control = "Textocomcontroles";
            CreateScrapRequest request = new CreateScrapRequest(control, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).isNotBlank();
        }

        @Test
        @DisplayName("Tag HTML incompleta / malformada — JSoup deve lidar sem crash")
        void shouldHandleMalformedHtml() {
            String malformed = "<div><b>Negrito sem fechar<i>Itálico<img src=x<a href=";
            CreateScrapRequest request = new CreateScrapRequest(malformed, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).isNotBlank();
        }

        @Test
        @DisplayName("Milhares de entidades HTML — &amp; repetida 500 vezes")
        void shouldHandleMassiveHtmlEntities() {
            String entities = "&amp;".repeat(200) + "texto";
            CreateScrapRequest request = new CreateScrapRequest(entities, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("texto");
        }

        @Test
        @DisplayName("Emoji composto (skin tone + ZWJ sequence) — deve ser preservado")
        void shouldPreserveComplexEmoji() {
            String complexEmoji = "Família: 👨‍👩‍👧‍👦 linda!";
            CreateScrapRequest request = new CreateScrapRequest(complexEmoji, maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.content()).contains("Família:");
            assertThat(response.content()).contains("linda!");
        }
    }

    @Nested
    @DisplayName("Paginação - Tamanho de página restrito a 10, 20 ou 50")
    class PageSizeValidation {

        @Test
        @DisplayName("Deve aceitar page size 10")
        void shouldAcceptPageSize10() {
            when(userRepository.existsById(maria.getId())).thenReturn(true);
            when(scrapRepository.findVisibleByOwnerId(any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<ScrapResponse> result = scrapService.findByOwner(maria.getId(), 0, 10);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve aceitar page size 20")
        void shouldAcceptPageSize20() {
            when(userRepository.existsById(maria.getId())).thenReturn(true);
            when(scrapRepository.findVisibleByOwnerId(any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<ScrapResponse> result = scrapService.findByOwner(maria.getId(), 0, 20);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve aceitar page size 50")
        void shouldAcceptPageSize50() {
            when(userRepository.existsById(maria.getId())).thenReturn(true);
            when(scrapRepository.findVisibleByOwnerId(any(), any(), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            Page<ScrapResponse> result = scrapService.findByOwner(maria.getId(), 0, 50);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve rejeitar page size 25")
        void shouldRejectPageSize25() {
            assertThatThrownBy(() -> scrapService.findByOwner(maria.getId(), 0, 25))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Page size must be one of");
        }

        @Test
        @DisplayName("Deve rejeitar page size 100")
        void shouldRejectPageSize100() {
            assertThatThrownBy(() -> scrapService.findByOwner(maria.getId(), 0, 100))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Page size must be one of");
        }

        @Test
        @DisplayName("Deve rejeitar page size 5")
        void shouldRejectPageSize5() {
            assertThatThrownBy(() -> scrapService.findSent(0, 5))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Page size must be one of");
        }

        @Test
        @DisplayName("Page size inválido no findSent também deve ser rejeitado")
        void shouldRejectInvalidPageSizeInFindSent() {
            assertThatThrownBy(() -> scrapService.findSent(0, 15))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Page size must be one of");
        }
    }

    @Nested
    @DisplayName("Delete em Lote - Excluir múltiplos recados selecionados")
    class BulkDelete {

        @Test
        @DisplayName("Deve deletar múltiplos scraps de uma vez")
        void shouldDeleteMultipleScraps() {
            Scrap scrap1 = buildScrap(felipe, maria, "Recado 1", false);
            Scrap scrap2 = buildScrap(felipe, maria, "Recado 2", false);

            when(scrapRepository.findById(scrap1.getId())).thenReturn(Optional.of(scrap1));
            when(scrapRepository.findById(scrap2.getId())).thenReturn(Optional.of(scrap2));

            scrapService.deleteMultiple(List.of(scrap1.getId(), scrap2.getId()));

            verify(scrapRepository).deleteById(scrap1.getId());
            verify(scrapRepository).deleteById(scrap2.getId());
        }

        @Test
        @DisplayName("Dono do mural pode deletar em lote scraps no seu mural")
        void wallOwnerCanBulkDeleteScrapsOnTheirWall() {
            Scrap scrap1 = buildScrap(maria, felipe, "Recado da Maria", false);
            Scrap scrap2 = buildScrap(joao, felipe, "Recado do João", false);

            when(scrapRepository.findById(scrap1.getId())).thenReturn(Optional.of(scrap1));
            when(scrapRepository.findById(scrap2.getId())).thenReturn(Optional.of(scrap2));

            scrapService.deleteMultiple(List.of(scrap1.getId(), scrap2.getId()));

            verify(scrapRepository).deleteById(scrap1.getId());
            verify(scrapRepository).deleteById(scrap2.getId());
        }

        @Test
        @DisplayName("Deve rejeitar se um dos scraps não pertence ao usuário")
        void shouldRejectIfOneScrapNotAuthorized() {
            Scrap myScrap = buildScrap(felipe, maria, "Meu recado", false);
            Scrap otherScrap = buildScrap(maria, joao, "Não é meu", false);

            when(scrapRepository.findById(myScrap.getId())).thenReturn(Optional.of(myScrap));
            when(scrapRepository.findById(otherScrap.getId())).thenReturn(Optional.of(otherScrap));

            assertThatThrownBy(() -> scrapService.deleteMultiple(List.of(myScrap.getId(), otherScrap.getId())))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Only the author or wall owner can delete");

            verify(scrapRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Deve rejeitar lista vazia de IDs")
        void shouldRejectEmptyList() {
            assertThatThrownBy(() -> scrapService.deleteMultiple(List.of()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No scrap IDs provided");
        }

        @Test
        @DisplayName("Deve rejeitar lista nula")
        void shouldRejectNullList() {
            assertThatThrownBy(() -> scrapService.deleteMultiple(null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No scrap IDs provided");
        }

        @Test
        @DisplayName("Deve falhar se um dos scraps não existe")
        void shouldFailIfOneScrapNotFound() {
            Scrap existing = buildScrap(felipe, maria, "Existe", false);
            UUID fakeId = UUID.randomUUID();

            when(scrapRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
            when(scrapRepository.findById(fakeId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.deleteMultiple(List.of(existing.getId(), fakeId)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }
    }

    @Nested
    @DisplayName("Responder Recado e Fio de Conversa")
    class ThreadAndReply {

        @Test
        @DisplayName("Deve criar scrap como resposta a outro (com parentId)")
        void shouldCreateReplyScrap() {
            Scrap parentScrap = buildScrap(maria, felipe, "Oi Felipe!", false);
            CreateScrapRequest request = new CreateScrapRequest(
                    "Oi Maria! Tudo bem?", maria.getId(), false, parentScrap.getId()
            );

            when(scrapRepository.findById(parentScrap.getId())).thenReturn(Optional.of(parentScrap));
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.parentId()).isEqualTo(parentScrap.getId());
            assertThat(response.content()).isEqualTo("Oi Maria! Tudo bem?");
        }

        @Test
        @DisplayName("Scrap sem parentId deve ter parentId null no response")
        void shouldHaveNullParentIdWhenNotReply() {
            CreateScrapRequest request = new CreateScrapRequest("Recado normal", maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.parentId()).isNull();
        }

        @Test
        @DisplayName("Deve rejeitar reply a scrap inexistente")
        void shouldRejectReplyToNonExistentScrap() {
            UUID fakeParentId = UUID.randomUUID();
            CreateScrapRequest request = new CreateScrapRequest(
                    "Resposta", maria.getId(), false, fakeParentId
            );
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            when(scrapRepository.findById(fakeParentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> scrapService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Scrap not found");
        }

        @Test
        @DisplayName("Deve retornar o fio de conversa completo em ordem cronológica")
        void shouldReturnThreadInChronologicalOrder() {
            Scrap root = buildScrap(maria, felipe, "Oi Felipe!", false);
            root.setParent(null);

            Scrap reply1 = buildScrap(felipe, maria, "Oi Maria!", false);
            reply1.setParent(root);

            Scrap reply2 = buildScrap(maria, felipe, "Tudo bem?", false);
            reply2.setParent(root);

            when(scrapRepository.findById(root.getId())).thenReturn(Optional.of(root));
            when(scrapRepository.findByParentIdOrderByCreatedAtAsc(root.getId()))
                    .thenReturn(List.of(reply1, reply2));
            when(scrapRepository.findByParentIdOrderByCreatedAtAsc(reply1.getId()))
                    .thenReturn(Collections.emptyList());
            when(scrapRepository.findByParentIdOrderByCreatedAtAsc(reply2.getId()))
                    .thenReturn(Collections.emptyList());

            List<ScrapResponse> thread = scrapService.findThread(root.getId());

            assertThat(thread).hasSize(3);
            assertThat(thread.get(0).content()).isEqualTo("Oi Felipe!");
            assertThat(thread.get(1).content()).isEqualTo("Oi Maria!");
            assertThat(thread.get(2).content()).isEqualTo("Tudo bem?");
        }

        @Test
        @DisplayName("Acessar thread a partir de uma reply deve navegar até a raiz")
        void shouldNavigateToRootFromReply() {
            Scrap root = buildScrap(maria, felipe, "Raiz", false);
            root.setParent(null);

            Scrap reply = buildScrap(felipe, maria, "Resposta", false);
            reply.setParent(root);

            when(scrapRepository.findById(reply.getId())).thenReturn(Optional.of(reply));
            when(scrapRepository.findByParentIdOrderByCreatedAtAsc(root.getId()))
                    .thenReturn(List.of(reply));
            when(scrapRepository.findByParentIdOrderByCreatedAtAsc(reply.getId()))
                    .thenReturn(Collections.emptyList());

            List<ScrapResponse> thread = scrapService.findThread(reply.getId());

            assertThat(thread).hasSize(2);
            assertThat(thread.get(0).content()).isEqualTo("Raiz");
            assertThat(thread.get(1).content()).isEqualTo("Resposta");
        }

        @Test
        @DisplayName("Thread de conversa privada deve ser bloqueada para terceiros")
        void shouldBlockPrivateThreadForThirdParty() {
            Scrap root = buildScrap(maria, joao, "Segredo", true);
            root.setParent(null);

            when(scrapRepository.findById(root.getId())).thenReturn(Optional.of(root));

            assertThatThrownBy(() -> scrapService.findThread(root.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("This conversation is private");
        }

        @Test
        @DisplayName("Thread de scrap sem respostas retorna apenas o scrap raiz")
        void shouldReturnSingleScrapThread() {
            Scrap root = buildScrap(maria, felipe, "Sem respostas", false);
            root.setParent(null);

            when(scrapRepository.findById(root.getId())).thenReturn(Optional.of(root));
            when(scrapRepository.findByParentIdOrderByCreatedAtAsc(root.getId()))
                    .thenReturn(Collections.emptyList());

            List<ScrapResponse> thread = scrapService.findThread(root.getId());

            assertThat(thread).hasSize(1);
            assertThat(thread.get(0).content()).isEqualTo("Sem respostas");
        }
    }

    @Nested
    @DisplayName("Marcar como Lido e Contagem de Não Lidos")
    class ReadStatus {

        @Test
        @DisplayName("Deve marcar scraps como lidos no mural do usuário autenticado")
        void shouldMarkScrapsAsRead() {
            Scrap scrap1 = buildScrap(maria, felipe, "Recado 1", false);
            Scrap scrap2 = buildScrap(joao, felipe, "Recado 2", false);
            List<UUID> ids = List.of(scrap1.getId(), scrap2.getId());

            when(scrapRepository.markAsRead(ids, felipe.getId())).thenReturn(2);

            int updated = scrapService.markAsRead(ids);

            assertThat(updated).isEqualTo(2);
            verify(scrapRepository).markAsRead(ids, felipe.getId());
        }

        @Test
        @DisplayName("Não deve marcar scraps de outro usuário como lidos (query filtra por owner_id)")
        void shouldOnlyMarkOwnScrapsAsRead() {
            Scrap scrapOnOtherWall = buildScrap(felipe, maria, "No mural da Maria", false);
            List<UUID> ids = List.of(scrapOnOtherWall.getId());

            when(scrapRepository.markAsRead(ids, felipe.getId())).thenReturn(0);

            int updated = scrapService.markAsRead(ids);

            assertThat(updated).isZero();
        }

        @Test
        @DisplayName("Marcar scrap já lido novamente não deve alterar (readAt IS NULL)")
        void shouldNotRemarkAlreadyReadScraps() {
            Scrap alreadyRead = buildScrap(maria, felipe, "Já lido", false);
            List<UUID> ids = List.of(alreadyRead.getId());

            when(scrapRepository.markAsRead(ids, felipe.getId())).thenReturn(0);

            int updated = scrapService.markAsRead(ids);

            assertThat(updated).isZero();
        }

        @Test
        @DisplayName("Deve rejeitar lista vazia de IDs para marcar como lido")
        void shouldRejectEmptyIdList() {
            assertThatThrownBy(() -> scrapService.markAsRead(List.of()))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No scrap IDs provided");
        }

        @Test
        @DisplayName("Deve rejeitar lista nula de IDs para marcar como lido")
        void shouldRejectNullIdList() {
            assertThatThrownBy(() -> scrapService.markAsRead(null))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("No scrap IDs provided");
        }

        @Test
        @DisplayName("Deve retornar contagem de scraps não lidos de um usuário")
        void shouldReturnUnreadCount() {
            when(userRepository.existsById(felipe.getId())).thenReturn(true);
            when(scrapRepository.countUnreadByOwnerId(felipe.getId())).thenReturn(5L);

            long count = scrapService.countUnread(felipe.getId());

            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("Deve retornar 0 quando não há scraps não lidos")
        void shouldReturnZeroWhenNoUnreadScraps() {
            when(userRepository.existsById(felipe.getId())).thenReturn(true);
            when(scrapRepository.countUnreadByOwnerId(felipe.getId())).thenReturn(0L);

            long count = scrapService.countUnread(felipe.getId());

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Deve retornar 404 para contagem de não lidos de usuário inexistente")
        void shouldReturn404ForUnreadCountOfNonExistentUser() {
            UUID fakeId = UUID.randomUUID();
            when(userRepository.existsById(fakeId)).thenReturn(false);

            assertThatThrownBy(() -> scrapService.countUnread(fakeId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("readAt deve ser null para scrap recém-criado")
        void shouldHaveNullReadAtForNewScrap() {
            CreateScrapRequest request = new CreateScrapRequest("Recado novo", maria.getId(), false, null);
            when(userRepository.findById(maria.getId())).thenReturn(Optional.of(maria));
            stubSaveScrap();

            ScrapResponse response = scrapService.create(request);

            assertThat(response.readAt()).isNull();
        }

        @Test
        @DisplayName("readAt deve aparecer no response quando scrap foi lido")
        void shouldShowReadAtInResponse() {
            Scrap scrap = buildScrap(maria, felipe, "Lido", false);
            java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
            scrap.setReadAt(now);

            when(scrapRepository.findById(scrap.getId())).thenReturn(Optional.of(scrap));

            ScrapResponse response = scrapService.findById(scrap.getId());

            assertThat(response.readAt()).isEqualTo(now);
        }
    }
}
