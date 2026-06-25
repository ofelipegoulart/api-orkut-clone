package com.orkutclone.api.service;

import com.orkutclone.api.dto.UpdateUserRequest;
import com.orkutclone.api.dto.UserResponse;
import com.orkutclone.api.model.User;
import com.orkutclone.api.model.enums.Role;
import com.orkutclone.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Felipe Goulart")
                .email("felipe@orkut.com")
                .password("encoded")
                .bio("Amando o Orkut!")
                .birthDate(LocalDate.of(1995, 3, 15))
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubFindAndSave() {
        when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("Ver meu perfil - Cliquei em 'Meu Perfil'")
    class GetCurrentUser {

        @Test
        @DisplayName("Deve retornar meus dados quando acesso meu perfil")
        void shouldReturnCurrentUserData() {
            UserResponse response = userService.getCurrentUser();

            assertThat(response.id()).isEqualTo(currentUser.getId());
            assertThat(response.name()).isEqualTo("Felipe Goulart");
            assertThat(response.email()).isEqualTo("felipe@orkut.com");
            assertThat(response.bio()).isEqualTo("Amando o Orkut!");
            assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 3, 15));
        }

        @Test
        @DisplayName("Deve lidar com campos opcionais nulos (bio, birthDate, profilePicture)")
        void shouldHandleNullOptionalFields() {
            currentUser.setBio(null);
            currentUser.setBirthDate(null);
            currentUser.setProfilePicture(null);
            currentUser.setCreatedAt(null);

            UserResponse response = userService.getCurrentUser();

            assertThat(response.bio()).isNull();
            assertThat(response.birthDate()).isNull();
            assertThat(response.profilePicture()).isNull();
            assertThat(response.avatar()).isNull();
            assertThat(response.createdAt()).isNull();
        }

        @Test
        @DisplayName("avatar deve ser sempre igual a profilePicture")
        void avatarShouldMirrorProfilePicture() {
            currentUser.setProfilePicture("data:image/png;base64,foto");

            UserResponse response = userService.getCurrentUser();

            assertThat(response.avatar()).isEqualTo(response.profilePicture());
        }
    }

    @Nested
    @DisplayName("Editar perfil - Atualizando minhas informações")
    class UpdateUser {

        @Test
        @DisplayName("Deve atualizar apenas o nome quando só o nome é informado")
        void shouldUpdateOnlyName() {
            UpdateUserRequest request = new UpdateUserRequest("Felipe G.", null, null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.name()).isEqualTo("Felipe G.");
            assertThat(response.bio()).isEqualTo("Amando o Orkut!");
        }

        @Test
        @DisplayName("Deve atualizar a bio sem mexer nos outros campos")
        void shouldUpdateOnlyBio() {
            UpdateUserRequest request = new UpdateUserRequest(null, "Saudades do Orkut antigo!", null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.bio()).isEqualTo("Saudades do Orkut antigo!");
            assertThat(response.name()).isEqualTo("Felipe Goulart");
        }

        @Test
        @DisplayName("Deve atualizar todos os campos de uma vez")
        void shouldUpdateAllFields() {
            UpdateUserRequest request = new UpdateUserRequest(
                    "Felipe Novo",
                    "Perfil atualizado!",
                    "data:image/png;base64,abc123",
                    LocalDate.of(1995, 6, 20)
            );
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.name()).isEqualTo("Felipe Novo");
            assertThat(response.bio()).isEqualTo("Perfil atualizado!");
            assertThat(response.profilePicture()).isEqualTo("data:image/png;base64,abc123");
            assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 6, 20));
        }

        @Test
        @DisplayName("Campos null não devem sobrescrever dados existentes")
        void shouldNotOverwriteWithNull() {
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.name()).isEqualTo("Felipe Goulart");
            assertThat(response.bio()).isEqualTo("Amando o Orkut!");
            assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 3, 15));
        }
    }

    @Nested
    @DisplayName("Validação de Nome - Nome não pode ficar vazio")
    class NameValidation {

        @Test
        @DisplayName("Deve rejeitar nome em branco (só espaços)")
        void shouldRejectBlankName() {
            UpdateUserRequest request = new UpdateUserRequest("   ", null, null, null);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            assertThatThrownBy(() -> userService.updateUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Name cannot be empty");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar nome vazio")
        void shouldRejectEmptyName() {
            UpdateUserRequest request = new UpdateUserRequest("", null, null, null);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            assertThatThrownBy(() -> userService.updateUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Name cannot be empty");
        }

        @Test
        @DisplayName("Deve aceitar nome com emojis e unicode")
        void shouldAcceptNameWithEmojisAndUnicode() {
            UpdateUserRequest request = new UpdateUserRequest("Felipe ✨ Goulart", null, null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.name()).isEqualTo("Felipe ✨ Goulart");
        }

        @Test
        @DisplayName("Deve aceitar nome com caracteres acentuados")
        void shouldAcceptAccentedName() {
            UpdateUserRequest request = new UpdateUserRequest("José María Ñoño", null, null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.name()).isEqualTo("José María Ñoño");
        }
    }

    @Nested
    @DisplayName("Validação de Bio - Quem sou eu (limite de 1024 chars)")
    class BioValidation {

        @Test
        @DisplayName("Deve rejeitar bio com mais de 1024 caracteres")
        void shouldRejectBioExceedingMaxLength() {
            String longBio = "a".repeat(1025);
            UpdateUserRequest request = new UpdateUserRequest(null, longBio, null, null);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            assertThatThrownBy(() -> userService.updateUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Bio exceeds maximum length of 1024");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve aceitar bio com exatamente 1024 caracteres")
        void shouldAcceptBioAtExactMaxLength() {
            String maxBio = "a".repeat(1024);
            UpdateUserRequest request = new UpdateUserRequest(null, maxBio, null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.bio()).hasSize(1024);
        }

        @Test
        @DisplayName("Deve aceitar bio vazia (limpar a bio)")
        void shouldAcceptEmptyBio() {
            UpdateUserRequest request = new UpdateUserRequest(null, "", null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.bio()).isEmpty();
        }

        @Test
        @DisplayName("Deve aceitar bio com emojis e caracteres unicode")
        void shouldAcceptBioWithEmojisAndUnicode() {
            String emojiBio = "Amando o Orkut! ❤️🎉 Café & música à tarde 🎵 ñ";
            UpdateUserRequest request = new UpdateUserRequest(null, emojiBio, null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.bio()).isEqualTo(emojiBio);
        }

        @Test
        @DisplayName("Deve aceitar bio com quebras de linha")
        void shouldAcceptBioWithLineBreaks() {
            String multiLineBio = "Linha 1\nLinha 2\nLinha 3";
            UpdateUserRequest request = new UpdateUserRequest(null, multiLineBio, null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.bio()).isEqualTo(multiLineBio);
        }

        @Test
        @DisplayName("Deve aceitar bio com apenas espaços (não é o mesmo que nome)")
        void shouldAcceptBioWithOnlySpaces() {
            UpdateUserRequest request = new UpdateUserRequest(null, "   ", null, null);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.bio()).isEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("Validação de Data de Nascimento")
    class BirthDateValidation {

        @Test
        @DisplayName("Deve rejeitar data de nascimento no futuro")
        void shouldRejectFutureBirthDate() {
            LocalDate futureDate = LocalDate.now().plusDays(1);
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, futureDate);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            assertThatThrownBy(() -> userService.updateUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Birth date cannot be in the future");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve rejeitar data de nascimento muito no futuro")
        void shouldRejectFarFutureBirthDate() {
            LocalDate farFuture = LocalDate.of(2090, 1, 1);
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, farFuture);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            assertThatThrownBy(() -> userService.updateUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Birth date cannot be in the future");
        }

        @Test
        @DisplayName("Deve aceitar data de nascimento de hoje (recém-nascido)")
        void shouldAcceptTodayAsBirthDate() {
            LocalDate today = LocalDate.now();
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, today);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.birthDate()).isEqualTo(today);
        }

        @Test
        @DisplayName("Deve aceitar data de nascimento no passado")
        void shouldAcceptPastBirthDate() {
            LocalDate pastDate = LocalDate.of(1990, 6, 15);
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, pastDate);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.birthDate()).isEqualTo(pastDate);
        }

        @Test
        @DisplayName("Deve aceitar data de nascimento de ontem")
        void shouldAcceptYesterdayAsBirthDate() {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            UpdateUserRequest request = new UpdateUserRequest(null, null, null, yesterday);
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.birthDate()).isEqualTo(yesterday);
        }
    }

    @Nested
    @DisplayName("Consistência - Múltiplas validações ao mesmo tempo")
    class Consistency {

        @Test
        @DisplayName("Deve rejeitar request com nome vazio mesmo que bio seja válida")
        void shouldRejectBlankNameEvenWithValidBio() {
            UpdateUserRequest request = new UpdateUserRequest("", "Bio válida", null, null);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));

            assertThatThrownBy(() -> userService.updateUser(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Name cannot be empty");
        }

        @Test
        @DisplayName("Update válido deve persistir todas as mudanças corretamente")
        void shouldPersistAllChangesCorrectly() {
            UpdateUserRequest request = new UpdateUserRequest(
                    "Nome Novo", "Bio nova", null, LocalDate.of(2000, 1, 1)
            );
            stubFindAndSave();

            UserResponse response = userService.updateUser(request);

            assertThat(response.name()).isEqualTo("Nome Novo");
            assertThat(response.bio()).isEqualTo("Bio nova");
            assertThat(response.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
            assertThat(response.email()).isEqualTo("felipe@orkut.com");
            verify(userRepository).save(any(User.class));
        }
    }
}
