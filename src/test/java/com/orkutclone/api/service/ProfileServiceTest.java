package com.orkutclone.api.service;

import com.orkutclone.api.dto.profile.*;
import com.orkutclone.api.model.*;
import com.orkutclone.api.model.enums.PrivacyLevel;
import com.orkutclone.api.model.enums.Role;
import com.orkutclone.api.repository.*;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository profileRepository;
    @Mock private UserProfileGeneralRepository generalRepository;
    @Mock private UserProfileSocialRepository socialRepository;
    @Mock private UserProfileContactRepository contactRepository;
    @Mock private UserProfileProfessionalRepository professionalRepository;
    @Mock private UserProfilePersonalRepository personalRepository;

    @InjectMocks
    private ProfileService profileService;

    private User currentUser;
    private UserProfile coreProfile;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Felipe Goulart")
                .email("felipe@orkut.com")
                .password("encoded")
                .role(Role.USER)
                .build();

        coreProfile = new UserProfile();
        coreProfile.setId(UUID.randomUUID());
        coreProfile.setUser(currentUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubCoreProfile() {
        when(profileRepository.findByUserId(currentUser.getId())).thenReturn(Optional.of(coreProfile));
    }

    private void stubCoreProfileAutoCreate() {
        when(profileRepository.findByUserId(currentUser.getId())).thenReturn(Optional.empty());
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> {
            UserProfile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
    }

    private UserProfileGeneral createGeneralWithDefaults() {
        UserProfileGeneral general = new UserProfileGeneral();
        general.setProfile(coreProfile);
        general.setLanguages(new ArrayList<>());
        general.setInterestedIn(new ArrayList<>());
        return general;
    }

    @Nested
    @DisplayName("Perfil Geral - Preenchendo minhas informações básicas")
    class GeneralProfile {

        @Test
        @DisplayName("Deve criar perfil geral automaticamente na primeira vez que acessa")
        void shouldAutoCreateGeneralProfileOnFirstAccess() {
            stubCoreProfile();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.empty());

            UserProfileGeneral newGeneral = createGeneralWithDefaults();
            when(generalRepository.save(any(UserProfileGeneral.class))).thenReturn(newGeneral);

            GeneralProfileDTO result = profileService.getGeneral();

            verify(generalRepository).save(any(UserProfileGeneral.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve criar o core profile E o general se nenhum existe")
        void shouldAutoCreateBothCoreAndGeneralProfile() {
            stubCoreProfileAutoCreate();

            when(generalRepository.findByProfileId(any())).thenReturn(Optional.empty());
            UserProfileGeneral newGeneral = new UserProfileGeneral();
            newGeneral.setProfile(coreProfile);
            newGeneral.setLanguages(new ArrayList<>());
            newGeneral.setInterestedIn(new ArrayList<>());
            when(generalRepository.save(any(UserProfileGeneral.class))).thenReturn(newGeneral);

            GeneralProfileDTO result = profileService.getGeneral();

            verify(profileRepository).save(any(UserProfile.class));
            verify(generalRepository).save(any(UserProfileGeneral.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve atualizar nome e sobrenome e sincronizar com User.name")
        void shouldUpdateNameAndSyncWithUser() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    "Felipe", "Silva", null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO result = profileService.updateGeneral(dto);

            assertThat(result.firstName()).isEqualTo("Felipe");
            assertThat(result.lastName()).isEqualTo("Silva");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getName()).isEqualTo("Felipe Silva");
        }

        @Test
        @DisplayName("Atualizar só firstName deve sincronizar com lastName existente")
        void shouldSyncNameWithExistingLastName() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            general.setFirstName("Velho");
            general.setLastName("Sobrenome");
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    "Novo", null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            profileService.updateGeneral(dto);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getName()).isEqualTo("Novo Sobrenome");
        }

        @Test
        @DisplayName("Não deve permitir firstName em branco")
        void shouldRejectBlankFirstName() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    "   ", null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("firstName cannot be empty");
        }

        @Test
        @DisplayName("Não deve permitir lastName em branco")
        void shouldRejectBlankLastName() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, "", null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("lastName cannot be empty");
        }

        @Test
        @DisplayName("Não deve permitir gender com valor inválido")
        void shouldRejectInvalidGender() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, "helicóptero", null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for gender");
        }

        @Test
        @DisplayName("Deve aceitar os 3 gêneros válidos: masculino, feminino, não binário")
        void shouldAcceptAllValidGenders() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            for (String gender : List.of("masculino", "feminino", "não binário")) {
                GeneralProfileDTO dto = new GeneralProfileDTO(
                        null, null, gender, null,
                        null, null, null, null, null,
                        null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null
                );
                GeneralProfileDTO result = profileService.updateGeneral(dto);
                assertThat(result.gender()).isEqualTo(gender);
            }
        }

        @Test
        @DisplayName("Não deve permitir country com valor inválido")
        void shouldRejectInvalidCountry() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, "Nárnia",
                    null, null, null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for country");
        }

        @Test
        @DisplayName("Deve atualizar idiomas e níveis de privacidade")
        void shouldUpdateLanguagesAndPrivacy() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    List.of("Português", "Inglês", "Espanhol"), PrivacyLevel.FRIENDS,
                    null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO result = profileService.updateGeneral(dto);

            assertThat(result.languages()).containsExactly("Português", "Inglês", "Espanhol");
            assertThat(result.languagesPrivacy()).isEqualTo(PrivacyLevel.FRIENDS);
        }

        @Test
        @DisplayName("Atualizar idiomas com lista vazia deve limpar a lista")
        void shouldClearLanguagesWithEmptyList() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            general.getLanguages().addAll(List.of("Português", "Inglês"));
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    List.of(), null,
                    null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO result = profileService.updateGeneral(dto);

            assertThat(result.languages()).isEmpty();
        }

        @Test
        @DisplayName("Deve atualizar dados de localização e educação em lote")
        void shouldUpdateLocationAndEducation() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, "solteiro",
                    "março", "15", PrivacyLevel.FRIENDS,
                    "1995", PrivacyLevel.ONLY_ME,
                    "São Paulo", "SP", "01234-567", "Brasil",
                    null, null,
                    "Colégio XYZ", PrivacyLevel.EVERYONE,
                    "USP", PrivacyLevel.EVERYONE,
                    "Google", PrivacyLevel.FRIENDS,
                    List.of("namoro"), "mulheres"
            );

            GeneralProfileDTO result = profileService.updateGeneral(dto);

            assertThat(result.city()).isEqualTo("São Paulo");
            assertThat(result.country()).isEqualTo("Brasil");
            assertThat(result.birthYear()).isEqualTo("1995");
            assertThat(result.birthYearPrivacy()).isEqualTo(PrivacyLevel.ONLY_ME);
            assertThat(result.highSchool()).isEqualTo("Colégio XYZ");
            assertThat(result.relationshipStatus()).isEqualTo("solteiro");
            assertThat(result.interestedIn()).containsExactly("namoro");
            assertThat(result.datingPreference()).isEqualTo("mulheres");
        }

        @Test
        @DisplayName("Campos null não devem alterar valores existentes no perfil")
        void shouldNotOverwriteExistingValuesWithNull() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            general.setFirstName("Felipe");
            general.setLastName("Goulart");
            general.setGender("masculino");
            general.setCity("São Paulo");
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO allNulls = new GeneralProfileDTO(
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO result = profileService.updateGeneral(allNulls);

            assertThat(result.firstName()).isEqualTo("Felipe");
            assertThat(result.lastName()).isEqualTo("Goulart");
            assertThat(result.gender()).isEqualTo("masculino");
            assertThat(result.city()).isEqualTo("São Paulo");
        }
    }

    @Nested
    @DisplayName("Perfil Social - Contando sobre minha vida")
    class SocialProfile {

        private UserProfileSocial createSocialWithDefaults() {
            UserProfileSocial social = new UserProfileSocial();
            social.setProfile(coreProfile);
            social.setHumor(new ArrayList<>());
            social.setStyle(new ArrayList<>());
            return social;
        }

        @Test
        @DisplayName("Deve atualizar interesses e estilo de vida completo")
        void shouldUpdateSocialInfo() {
            stubCoreProfile();
            UserProfileSocial social = createSocialWithDefaults();
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));
            when(socialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SocialProfileDTO dto = new SocialProfileDTO(
                    null, null, null, null,
                    null, null,
                    List.of("seco/sarcástico", "inteligente/sagaz"), List.of("alternativo", "casual"),
                    "não", "socialmente", "tenho gatos", null,
                    "São Paulo", "https://meusite.com",
                    "Amo programação e música!", "Código, café e rock",
                    "Futebol", "Programar, ler, cozinhar",
                    "O Senhor dos Anéis", "Rock, MPB, Jazz",
                    "Breaking Bad, The Office", "Matrix, Interestelar",
                    "Italiana, Japonesa"
            );

            SocialProfileDTO result = profileService.updateSocial(dto);

            assertThat(result.humor()).containsExactly("seco/sarcástico", "inteligente/sagaz");
            assertThat(result.style()).containsExactly("alternativo", "casual");
            assertThat(result.aboutMe()).isEqualTo("Amo programação e música!");
            assertThat(result.smoking()).isEqualTo("não");
            assertThat(result.music()).isEqualTo("Rock, MPB, Jazz");
        }

        @Test
        @DisplayName("Deve criar perfil social automaticamente na primeira vez")
        void shouldAutoCreateSocialProfile() {
            stubCoreProfile();
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.empty());

            UserProfileSocial newSocial = createSocialWithDefaults();
            when(socialRepository.save(any(UserProfileSocial.class))).thenReturn(newSocial);

            SocialProfileDTO result = profileService.getSocial();

            verify(socialRepository).save(any(UserProfileSocial.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve atualizar campos sensíveis com privacidade")
        void shouldUpdateSensitiveFieldsWithPrivacy() {
            stubCoreProfile();
            UserProfileSocial social = createSocialWithDefaults();
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));
            when(socialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SocialProfileDTO dto = new SocialProfileDTO(
                    "não", "caucasiano (branco)", "Agnóstico", "centrista",
                    "heterossexual", PrivacyLevel.ONLY_ME,
                    null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null
            );

            SocialProfileDTO result = profileService.updateSocial(dto);

            assertThat(result.sexualOrientation()).isEqualTo("heterossexual");
            assertThat(result.sexualOrientationPrivacy()).isEqualTo(PrivacyLevel.ONLY_ME);
            assertThat(result.religion()).isEqualTo("Agnóstico");
            assertThat(result.ethnicity()).isEqualTo("caucasiano (branco)");
        }

        @Test
        @DisplayName("Limpar humor e style com lista vazia")
        void shouldClearCollectionsWithEmptyList() {
            stubCoreProfile();
            UserProfileSocial social = createSocialWithDefaults();
            social.getHumor().addAll(List.of("Sarcástico", "Espirituoso"));
            social.getStyle().addAll(List.of("Casual"));
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));
            when(socialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SocialProfileDTO dto = new SocialProfileDTO(
                    null, null, null, null, null, null,
                    List.of(), List.of(),
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null
            );

            SocialProfileDTO result = profileService.updateSocial(dto);

            assertThat(result.humor()).isEmpty();
            assertThat(result.style()).isEmpty();
        }

        @Test
        @DisplayName("Deve preservar campos existentes quando só atualiza parcialmente")
        void shouldPreserveExistingFieldsOnPartialUpdate() {
            stubCoreProfile();
            UserProfileSocial social = createSocialWithDefaults();
            social.setAboutMe("Texto anterior");
            social.setSmoking("não");
            social.setHometown("Rio de Janeiro");
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));
            when(socialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SocialProfileDTO dto = new SocialProfileDTO(
                    null, null, null, null, null, null,
                    null, null,
                    null, "socialmente", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null
            );

            SocialProfileDTO result = profileService.updateSocial(dto);

            assertThat(result.drinking()).isEqualTo("socialmente");
            assertThat(result.aboutMe()).isEqualTo("Texto anterior");
            assertThat(result.smoking()).isEqualTo("não");
            assertThat(result.hometown()).isEqualTo("Rio de Janeiro");
        }
    }

    @Nested
    @DisplayName("Perfil de Contato - Configurando como me encontrar")
    class ContactProfile {

        private UserProfileContact createContactWithDefaults() {
            UserProfileContact contact = new UserProfileContact();
            contact.setProfile(coreProfile);
            contact.setSecondaryEmails(new ArrayList<>());
            return contact;
        }

        @Test
        @DisplayName("Deve atualizar email primário com nível de privacidade")
        void shouldUpdatePrimaryEmailWithPrivacy() {
            stubCoreProfile();
            UserProfileContact contact = createContactWithDefaults();
            when(contactRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(contact));
            when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContactProfileDTO dto = new ContactProfileDTO(
                    "felipe@orkut.com", PrivacyLevel.FRIENDS, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null
            );

            ContactProfileDTO result = profileService.updateContact(dto);

            assertThat(result.primaryEmail()).isEqualTo("felipe@orkut.com");
            assertThat(result.primaryEmailPrivacy()).isEqualTo(PrivacyLevel.FRIENDS);
        }

        @Test
        @DisplayName("Deve gerenciar emails secundários com privacidade individual")
        void shouldManageSecondaryEmails() {
            stubCoreProfile();
            UserProfileContact contact = createContactWithDefaults();
            when(contactRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(contact));
            when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<SecondaryEmailDTO> secondaryEmails = List.of(
                    new SecondaryEmailDTO("pessoal@gmail.com", PrivacyLevel.ONLY_ME),
                    new SecondaryEmailDTO("trabalho@empresa.com", PrivacyLevel.EVERYONE)
            );

            ContactProfileDTO dto = new ContactProfileDTO(
                    null, null, secondaryEmails,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null
            );

            ContactProfileDTO result = profileService.updateContact(dto);

            assertThat(result.secondaryEmails()).hasSize(2);
            assertThat(result.secondaryEmails().get(0).email()).isEqualTo("pessoal@gmail.com");
            assertThat(result.secondaryEmails().get(0).privacy()).isEqualTo(PrivacyLevel.ONLY_ME);
        }

        @Test
        @DisplayName("Atualizar emails secundários substitui a lista toda (não acumula)")
        void shouldReplaceSecondaryEmailsNotAppend() {
            stubCoreProfile();
            UserProfileContact contact = createContactWithDefaults();
            contact.getSecondaryEmails().add(new SecondaryEmail("antigo@email.com", PrivacyLevel.EVERYONE));
            when(contactRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(contact));
            when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<SecondaryEmailDTO> newEmails = List.of(
                    new SecondaryEmailDTO("novo@email.com", PrivacyLevel.FRIENDS)
            );

            ContactProfileDTO dto = new ContactProfileDTO(
                    null, null, newEmails,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null
            );

            ContactProfileDTO result = profileService.updateContact(dto);

            assertThat(result.secondaryEmails()).hasSize(1);
            assertThat(result.secondaryEmails().get(0).email()).isEqualTo("novo@email.com");
        }

        @Test
        @DisplayName("Deve atualizar endereço completo")
        void shouldUpdateFullAddress() {
            stubCoreProfile();
            UserProfileContact contact = createContactWithDefaults();
            when(contactRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(contact));
            when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContactProfileDTO dto = new ContactProfileDTO(
                    null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    "Rua das Flores, 123", "Apto 45",
                    "São Paulo", "SP", "01234-567", "Brasil"
            );

            ContactProfileDTO result = profileService.updateContact(dto);

            assertThat(result.address1()).isEqualTo("Rua das Flores, 123");
            assertThat(result.address2()).isEqualTo("Apto 45");
            assertThat(result.addressCity()).isEqualTo("São Paulo");
            assertThat(result.addressState()).isEqualTo("SP");
            assertThat(result.addressZipCode()).isEqualTo("01234-567");
            assertThat(result.addressCountry()).isEqualTo("Brasil");
        }

        @Test
        @DisplayName("Deve atualizar telefones com privacidade")
        void shouldUpdatePhonesWithPrivacy() {
            stubCoreProfile();
            UserProfileContact contact = createContactWithDefaults();
            when(contactRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(contact));
            when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContactProfileDTO dto = new ContactProfileDTO(
                    null, null, null,
                    null, null, null, null,
                    "(11) 3333-4444", PrivacyLevel.FRIENDS,
                    "(11) 99999-8888", PrivacyLevel.ONLY_ME,
                    null, null, null, null, null, null
            );

            ContactProfileDTO result = profileService.updateContact(dto);

            assertThat(result.homePhone()).isEqualTo("(11) 3333-4444");
            assertThat(result.homePhonePrivacy()).isEqualTo(PrivacyLevel.FRIENDS);
            assertThat(result.mobilePhone()).isEqualTo("(11) 99999-8888");
            assertThat(result.mobilePhonePrivacy()).isEqualTo(PrivacyLevel.ONLY_ME);
        }

        @Test
        @DisplayName("Deve atualizar contas de instant messaging com privacidade")
        void shouldUpdateImAccountsWithPrivacy() {
            stubCoreProfile();
            UserProfileContact contact = createContactWithDefaults();
            when(contactRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(contact));
            when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContactProfileDTO dto = new ContactProfileDTO(
                    null, null, null,
                    "felipe_msn@hotmail.com", PrivacyLevel.FRIENDS,
                    "felipe_icq_123456", PrivacyLevel.EVERYONE,
                    null, null, null, null,
                    null, null, null, null, null, null
            );

            ContactProfileDTO result = profileService.updateContact(dto);

            assertThat(result.im1()).isEqualTo("felipe_msn@hotmail.com");
            assertThat(result.im1Privacy()).isEqualTo(PrivacyLevel.FRIENDS);
            assertThat(result.im2()).isEqualTo("felipe_icq_123456");
        }
    }

    @Nested
    @DisplayName("Perfil Profissional - Minha carreira")
    class ProfessionalProfile {

        @Test
        @DisplayName("Deve atualizar informações profissionais completas")
        void shouldUpdateProfessionalInfo() {
            stubCoreProfile();
            UserProfileProfessional prof = new UserProfileProfessional();
            prof.setProfile(coreProfile);
            when(professionalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(prof));
            when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProfessionalProfileDTO dto = new ProfessionalProfileDTO(
                    "graduação", "Colégio XYZ", "USP",
                    "Ciência da Computação", "Bacharelado", "2017",
                    "Desenvolvedor", "Tecnologia", "Google",
                    "Desenvolvendo APIs REST", "(11) 99999-0000",
                    "Java, Spring, React", "Open Source, AI"
            );

            ProfessionalProfileDTO result = profileService.updateProfessional(dto);

            assertThat(result.profession()).isEqualTo("Desenvolvedor");
            assertThat(result.company()).isEqualTo("Google");
            assertThat(result.college()).isEqualTo("USP");
            assertThat(result.course()).isEqualTo("Ciência da Computação");
        }

        @Test
        @DisplayName("Deve criar perfil profissional automaticamente na primeira vez")
        void shouldAutoCreateProfessionalProfile() {
            stubCoreProfile();
            when(professionalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.empty());

            UserProfileProfessional newProf = new UserProfileProfessional();
            newProf.setProfile(coreProfile);
            when(professionalRepository.save(any(UserProfileProfessional.class))).thenReturn(newProf);

            ProfessionalProfileDTO result = profileService.getProfessional();

            verify(professionalRepository).save(any(UserProfileProfessional.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Deve atualizar parcialmente sem afetar campos existentes")
        void shouldPartiallyUpdateProfessional() {
            stubCoreProfile();
            UserProfileProfessional prof = new UserProfileProfessional();
            prof.setProfile(coreProfile);
            prof.setProfession("Desenvolvedor");
            prof.setCompany("Empresa Antiga");
            prof.setSector("Tecnologia");
            when(professionalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(prof));
            when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProfessionalProfileDTO dto = new ProfessionalProfileDTO(
                    null, null, null, null, null, null,
                    null, null, "Empresa Nova",
                    null, null, null, null
            );

            ProfessionalProfileDTO result = profileService.updateProfessional(dto);

            assertThat(result.company()).isEqualTo("Empresa Nova");
            assertThat(result.profession()).isEqualTo("Desenvolvedor");
            assertThat(result.sector()).isEqualTo("Tecnologia");
        }
    }

    @Nested
    @DisplayName("Perfil Pessoal - Quem eu sou")
    class PersonalProfile {

        private UserProfilePersonal createPersonalWithDefaults() {
            UserProfilePersonal personal = new UserProfilePersonal();
            personal.setProfile(coreProfile);
            personal.setAttractions(new ArrayList<>());
            return personal;
        }

        @Test
        @DisplayName("Deve atualizar características pessoais e atrações")
        void shouldUpdatePersonalTraitsAndAttractions() {
            stubCoreProfile();
            UserProfilePersonal personal = createPersonalWithDefaults();
            when(personalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(personal));
            when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PersonalProfileDTO dto = new PersonalProfileDTO(
                    "castanhos", "preto", "1.80m",
                    "atlético(a)", "atraente", "tatuagem visível",
                    "Alguém inteligente e divertido",
                    List.of("inteligência", "sarcasmo", "flertar"),
                    "Falsidade", "Um café e boa conversa",
                    "Comunicação é tudo",
                    "O sorriso", "olhos",
                    "Família, saúde, amor, amigos, música",
                    "Violão, livros, café, notebook, plantas"
            );

            PersonalProfileDTO result = profileService.updatePersonal(dto);

            assertThat(result.eyeColor()).isEqualTo("castanhos");
            assertThat(result.attractions()).containsExactly("inteligência", "sarcasmo", "flertar");
            assertThat(result.perfectMatch()).isEqualTo("Alguém inteligente e divertido");
            assertThat(result.bodyArt()).isEqualTo("tatuagem visível");
            assertThat(result.fiveEssentials()).isEqualTo("Família, saúde, amor, amigos, música");
        }

        @Test
        @DisplayName("Deve criar perfil pessoal automaticamente na primeira vez")
        void shouldAutoCreatePersonalProfile() {
            stubCoreProfile();
            when(personalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.empty());

            UserProfilePersonal newPersonal = createPersonalWithDefaults();
            when(personalRepository.save(any(UserProfilePersonal.class))).thenReturn(newPersonal);

            PersonalProfileDTO result = profileService.getPersonal();

            verify(personalRepository).save(any(UserProfilePersonal.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Atualizar atrações substitui a lista, não acumula")
        void shouldReplaceAttractionsNotAppend() {
            stubCoreProfile();
            UserProfilePersonal personal = createPersonalWithDefaults();
            personal.getAttractions().addAll(List.of("Inteligência", "Humor"));
            when(personalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(personal));
            when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PersonalProfileDTO dto = new PersonalProfileDTO(
                    null, null, null, null, null, null, null,
                    List.of("poder", "aventura"),
                    null, null, null, null, null, null, null
            );

            PersonalProfileDTO result = profileService.updatePersonal(dto);

            assertThat(result.attractions()).containsExactly("poder", "aventura");
            assertThat(result.attractions()).doesNotContain("inteligência", "sarcasmo");
        }

        @Test
        @DisplayName("Deve preservar campos existentes em update parcial")
        void shouldPreserveExistingFieldsOnPartialUpdate() {
            stubCoreProfile();
            UserProfilePersonal personal = createPersonalWithDefaults();
            personal.setEyeColor("azuis");
            personal.setHairColor("loiro");
            personal.setHeight("1.75m");
            when(personalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(personal));
            when(personalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PersonalProfileDTO dto = new PersonalProfileDTO(
                    null, "castanho claro", null,
                    null, null, null, null, null,
                    null, null, null, null, null, null, null
            );

            PersonalProfileDTO result = profileService.updatePersonal(dto);

            assertThat(result.hairColor()).isEqualTo("castanho claro");
            assertThat(result.eyeColor()).isEqualTo("azuis");
            assertThat(result.height()).isEqualTo("1.75m");
        }
    }

    @Nested
    @DisplayName("Avatar - Minha foto de perfil")
    class AvatarUpload {

        private String createValidBase64Image() throws Exception {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        }

        private String createImageOfSize(int width, int height) throws Exception {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        }

        @Test
        @DisplayName("Deve fazer upload de avatar PNG válido")
        void shouldUploadValidPngAvatar() throws Exception {
            String validImage = createValidBase64Image();
            AvatarRequest request = new AvatarRequest(validImage);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AvatarResponse response = profileService.uploadAvatar(request);

            assertThat(response.avatar()).isEqualTo(validImage);
            verify(userRepository).save(currentUser);
            assertThat(currentUser.getProfilePicture()).isEqualTo(validImage);
        }

        @Test
        @DisplayName("Deve fazer upload de avatar JPEG válido")
        void shouldUploadValidJpegAvatar() throws Exception {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            String jpegImage = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());

            AvatarRequest request = new AvatarRequest(jpegImage);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AvatarResponse response = profileService.uploadAvatar(request);

            assertThat(response.avatar()).isEqualTo(jpegImage);
        }

        @Test
        @DisplayName("Deve rejeitar formato de imagem inválido (text/plain)")
        void shouldRejectInvalidImageFormat() {
            AvatarRequest request = new AvatarRequest("data:text/plain;base64,abc123");

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid image format");
        }

        @Test
        @DisplayName("Deve rejeitar string que não é data URI")
        void shouldRejectNonDataUri() {
            AvatarRequest request = new AvatarRequest("https://example.com/photo.jpg");

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid image format");
        }

        @Test
        @DisplayName("Deve rejeitar base64 inválido")
        void shouldRejectInvalidBase64() {
            AvatarRequest request = new AvatarRequest("data:image/png;base64,!!!invalid!!!");

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid base64 data");
        }

        @Test
        @DisplayName("Deve rejeitar imagem menor que 32x32")
        void shouldRejectImageSmallerThanMinimum() throws Exception {
            String tinyImage = createImageOfSize(16, 16);
            AvatarRequest request = new AvatarRequest(tinyImage);

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("at least 32x32");
        }

        @Test
        @DisplayName("Deve aceitar imagem exatamente 32x32 (mínimo)")
        void shouldAcceptMinimumSizeImage() throws Exception {
            String minImage = createImageOfSize(32, 32);
            AvatarRequest request = new AvatarRequest(minImage);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AvatarResponse response = profileService.uploadAvatar(request);

            assertThat(response.avatar()).isNotNull();
        }

        @Test
        @DisplayName("Deve rejeitar imagem com largura OK mas altura menor")
        void shouldRejectImageWithInsufficientHeight() throws Exception {
            String badImage = createImageOfSize(64, 20);
            AvatarRequest request = new AvatarRequest(badImage);

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("at least 32x32");
        }

        @Test
        @DisplayName("Deve rejeitar formato webp não suportado")
        void shouldRejectUnsupportedFormat() {
            AvatarRequest request = new AvatarRequest("data:image/webp;base64,UklGRiIAAABXRUJQ");

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid image format");
        }

        @Test
        @DisplayName("Deve rejeitar string vazia como data URI")
        void shouldRejectEmptyString() {
            AvatarRequest request = new AvatarRequest("");

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Invalid image format");
        }

        @Test
        @DisplayName("Deve deletar avatar existente")
        void shouldDeleteExistingAvatar() {
            currentUser.setProfilePicture("data:image/png;base64,existingimage");
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            profileService.deleteAvatar();

            assertThat(currentUser.getProfilePicture()).isNull();
            verify(userRepository).save(currentUser);
        }

        @Test
        @DisplayName("Deletar avatar quando já não tem foto deve funcionar sem erro")
        void shouldHandleDeleteWhenNoAvatar() {
            currentUser.setProfilePicture(null);
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            profileService.deleteAvatar();

            assertThat(currentUser.getProfilePicture()).isNull();
            verify(userRepository).save(currentUser);
        }
    }

    @Nested
    @DisplayName("Concorrência e Idempotência - Avatar")
    class AvatarConcurrencyAndIdempotency {

        private String createValidBase64Image() throws Exception {
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        }

        @Test
        @DisplayName("Upload do mesmo avatar duas vezes — ambos devem funcionar")
        void shouldAllowDuplicateAvatarUpload() throws Exception {
            String validImage = createValidBase64Image();
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            AvatarResponse first = profileService.uploadAvatar(new AvatarRequest(validImage));
            AvatarResponse second = profileService.uploadAvatar(new AvatarRequest(validImage));

            assertThat(first.avatar()).isEqualTo(second.avatar());
            assertThat(currentUser.getProfilePicture()).isEqualTo(validImage);
        }

        @Test
        @DisplayName("Delete avatar duas vezes — segunda vez não causa erro")
        void shouldHandleDoubleDeleteGracefully() {
            currentUser.setProfilePicture("data:image/png;base64,foto");
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            profileService.deleteAvatar();
            assertThat(currentUser.getProfilePicture()).isNull();

            profileService.deleteAvatar();
            assertThat(currentUser.getProfilePicture()).isNull();
        }

        @Test
        @DisplayName("Upload → delete → upload — ciclo completo funciona")
        void shouldHandleUploadDeleteUploadCycle() throws Exception {
            String image1 = createValidBase64Image();
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            profileService.uploadAvatar(new AvatarRequest(image1));
            assertThat(currentUser.getProfilePicture()).isEqualTo(image1);

            profileService.deleteAvatar();
            assertThat(currentUser.getProfilePicture()).isNull();

            profileService.uploadAvatar(new AvatarRequest(image1));
            assertThat(currentUser.getProfilePicture()).isEqualTo(image1);
        }

        @Test
        @DisplayName("Trocar de avatar sobrescreve o anterior — só existe uma foto principal")
        void shouldOverwritePreviousAvatar() throws Exception {
            String image1 = createValidBase64Image();
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            profileService.uploadAvatar(new AvatarRequest(image1));
            String firstPicture = currentUser.getProfilePicture();

            BufferedImage img2 = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            ImageIO.write(img2, "png", baos2);
            String image2 = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos2.toByteArray());

            profileService.uploadAvatar(new AvatarRequest(image2));

            assertThat(currentUser.getProfilePicture()).isEqualTo(image2);
            assertThat(currentUser.getProfilePicture()).isNotEqualTo(firstPicture);
        }
    }

    @Nested
    @DisplayName("Concorrência e Idempotência - Perfil")
    class ProfileConcurrencyAndIdempotency {

        @Test
        @DisplayName("Mesmo update de perfil geral aplicado duas vezes — resultado idêntico")
        void shouldBeIdempotentOnRepeatedGeneralUpdate() {
            stubCoreProfile();
            UserProfileGeneral general = new UserProfileGeneral();
            general.setProfile(coreProfile);
            general.setLanguages(new ArrayList<>());
            general.setInterestedIn(new ArrayList<>());
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    "Felipe", "Silva", null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO first = profileService.updateGeneral(dto);
            GeneralProfileDTO second = profileService.updateGeneral(dto);

            assertThat(first.firstName()).isEqualTo(second.firstName());
            assertThat(first.lastName()).isEqualTo(second.lastName());
        }

        @Test
        @DisplayName("Dois updates parciais sequenciais — ambos os campos persistem")
        void shouldPreserveBothFieldsFromSequentialPartialUpdates() {
            stubCoreProfile();
            UserProfileSocial social = new UserProfileSocial();
            social.setProfile(coreProfile);
            social.setHumor(new ArrayList<>());
            social.setStyle(new ArrayList<>());
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));
            when(socialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SocialProfileDTO update1 = new SocialProfileDTO(
                    null, null, null, null, null, null,
                    null, null,
                    "não", null, null, null,
                    null, null, "Sobre mim", null,
                    null, null, null, null, null, null, null
            );
            profileService.updateSocial(update1);

            SocialProfileDTO update2 = new SocialProfileDTO(
                    null, null, null, null, null, null,
                    null, null,
                    null, "socialmente", null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null
            );
            SocialProfileDTO result = profileService.updateSocial(update2);

            assertThat(result.smoking()).isEqualTo("não");
            assertThat(result.drinking()).isEqualTo("socialmente");
            assertThat(result.aboutMe()).isEqualTo("Sobre mim");
        }
    }

    @Nested
    @DisplayName("Ataques Reais - Avatar")
    class AvatarAttacks {

        @Test
        @DisplayName("Base64 bomb — data URI válida mas dados enormes (>10MB) deve ser rejeitada")
        void shouldRejectBase64Bomb() {
            byte[] bomb = new byte[11 * 1024 * 1024];
            String bigBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bomb);
            AvatarRequest request = new AvatarRequest(bigBase64);

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("10MB");
        }

        @Test
        @DisplayName("Data URI com MIME type spoofado (diz PNG mas dados são lixo)")
        void shouldRejectSpoofedMimeType() {
            String spoofed = "data:image/png;base64," + Base64.getEncoder().encodeToString("isto nao eh uma imagem".getBytes());
            AvatarRequest request = new AvatarRequest(spoofed);

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Data URI com null bytes dentro do base64")
        void shouldHandleNullBytesInBase64() {
            byte[] withNulls = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            String nullBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(withNulls);
            AvatarRequest request = new AvatarRequest(nullBase64);

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Data URI com padding base64 malformado")
        void shouldRejectMalformedBase64Padding() {
            AvatarRequest request = new AvatarRequest("data:image/png;base64,YWJj===");

            assertThatThrownBy(() -> profileService.uploadAvatar(request))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    @Nested
    @DisplayName("Ataques Reais - Campos de Perfil")
    class ProfileFieldAttacks {

        @Test
        @DisplayName("Unicode zero-width characters no firstName — deve aceitar mas não causar crash")
        void shouldHandleZeroWidthInName() {
            stubCoreProfile();
            UserProfileGeneral general = new UserProfileGeneral();
            general.setProfile(coreProfile);
            general.setLanguages(new ArrayList<>());
            general.setInterestedIn(new ArrayList<>());
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(currentUser.getId())).thenReturn(Optional.of(currentUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    "Fe​li​pe", null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO result = profileService.updateGeneral(dto);

            assertThat(result.firstName()).contains("Fe");
        }

        @Test
        @DisplayName("Homoglyph attack no campo de email — deve aceitar sem crash")
        void shouldHandleHomoglyphInEmail() {
            stubCoreProfile();
            UserProfileContact contact = new UserProfileContact();
            contact.setProfile(coreProfile);
            contact.setSecondaryEmails(new ArrayList<>());
            when(contactRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(contact));
            when(contactRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ContactProfileDTO dto = new ContactProfileDTO(
                    "fеlіpe@оrkut.cоm", PrivacyLevel.FRIENDS, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null
            );

            ContactProfileDTO result = profileService.updateContact(dto);

            assertThat(result.primaryEmail()).isEqualTo("fеlіpe@оrkut.cоm");
        }

        @Test
        @DisplayName("String enorme (10KB) em campo aboutMe — deve aceitar (sem limite no service)")
        void shouldAcceptLargeAboutMe() {
            stubCoreProfile();
            UserProfileSocial social = new UserProfileSocial();
            social.setProfile(coreProfile);
            social.setHumor(new ArrayList<>());
            social.setStyle(new ArrayList<>());
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));
            when(socialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            String largeAbout = "A".repeat(10_000);
            SocialProfileDTO dto = new SocialProfileDTO(
                    null, null, null, null, null, null,
                    null, null,
                    null, null, null, null,
                    null, null, largeAbout, null,
                    null, null, null, null, null, null, null
            );

            SocialProfileDTO result = profileService.updateSocial(dto);

            assertThat(result.aboutMe()).hasSize(10_000);
        }

        @Test
        @DisplayName("Caracteres de controle em campos de perfil profissional")
        void shouldHandleControlCharsInProfessionalFields() {
            stubCoreProfile();
            UserProfileProfessional prof = new UserProfileProfessional();
            prof.setProfile(coreProfile);
            when(professionalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(prof));
            when(professionalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProfessionalProfileDTO dto = new ProfessionalProfileDTO(
                    null, null, null, null, null, null,
                    "Dev\teloper\n", "Tech\r\nnology", null,
                    null, null, null, null
            );

            ProfessionalProfileDTO result = profileService.updateProfessional(dto);

            assertThat(result.profession()).contains("Dev");
            assertThat(result.sector()).contains("Tech");
        }

        @Test
        @DisplayName("Lista de idiomas com todos os valores válidos — deve aceitar")
        void shouldAcceptAllValidLanguages() {
            stubCoreProfile();
            UserProfileGeneral general = new UserProfileGeneral();
            general.setProfile(coreProfile);
            general.setLanguages(new ArrayList<>());
            general.setInterestedIn(new ArrayList<>());
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<String> allLanguages = new ArrayList<>(com.orkutclone.api.validation.AllowedProfileValues.LANGUAGES);

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    allLanguages, null,
                    null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO result = profileService.updateGeneral(dto);

            assertThat(result.languages()).hasSize(allLanguages.size());
        }

        @Test
        @DisplayName("Idioma inválido na lista deve ser rejeitado")
        void shouldRejectInvalidLanguage() {
            stubCoreProfile();
            UserProfileGeneral general = new UserProfileGeneral();
            general.setProfile(coreProfile);
            general.setLanguages(new ArrayList<>());
            general.setInterestedIn(new ArrayList<>());
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    null, null, null, null, null,
                    null, null, null, null,
                    List.of("Português", "Klingon"), null,
                    null, null, null, null, null, null, null, null
            );

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for languages");
        }

        @Test
        @DisplayName("Right-to-left override em campo de cidade — não causa crash")
        void shouldHandleRtlInCityField() {
            stubCoreProfile();
            UserProfileGeneral general = new UserProfileGeneral();
            general.setProfile(coreProfile);
            general.setLanguages(new ArrayList<>());
            general.setInterestedIn(new ArrayList<>());
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    null, null, null, null, null,
                    "‮São Paulo‬", null, null, null,
                    null, null, null, null, null, null, null, null, null, null
            );

            GeneralProfileDTO result = profileService.updateGeneral(dto);

            assertThat(result.city()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validação de Valores Permitidos - Campos com opções fixas")
    class AllowedValuesValidation {

        @Test
        @DisplayName("Deve rejeitar relationshipStatus inválido")
        void shouldRejectInvalidRelationshipStatus() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, "enrolado",
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for relationshipStatus");
        }

        @Test
        @DisplayName("Deve rejeitar birthMonth inválido")
        void shouldRejectInvalidBirthMonth() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    "fevreiro", null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for birthMonth");
        }

        @Test
        @DisplayName("Deve rejeitar birthDay inválido (32)")
        void shouldRejectInvalidBirthDay() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));

            GeneralProfileDTO dto = new GeneralProfileDTO(
                    null, null, null, null,
                    null, "32", null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updateGeneral(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for birthDay");
        }

        @Test
        @DisplayName("Deve rejeitar smoking inválido")
        void shouldRejectInvalidSmoking() {
            stubCoreProfile();
            UserProfileSocial social = new UserProfileSocial();
            social.setProfile(coreProfile);
            social.setHumor(new ArrayList<>());
            social.setStyle(new ArrayList<>());
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));

            SocialProfileDTO dto = new SocialProfileDTO(
                    null, null, null, null, null, null, null, null,
                    "de vez em quando", null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updateSocial(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for smoking");
        }

        @Test
        @DisplayName("Deve rejeitar humor com valor fora da lista")
        void shouldRejectInvalidHumorOption() {
            stubCoreProfile();
            UserProfileSocial social = new UserProfileSocial();
            social.setProfile(coreProfile);
            social.setHumor(new ArrayList<>());
            social.setStyle(new ArrayList<>());
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));

            SocialProfileDTO dto = new SocialProfileDTO(
                    null, null, null, null, null, null,
                    List.of("seco/sarcástico", "HUMOR_INVENTADO"), null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updateSocial(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for humor");
        }

        @Test
        @DisplayName("Deve rejeitar eyeColor inválido")
        void shouldRejectInvalidEyeColor() {
            stubCoreProfile();
            UserProfilePersonal personal = new UserProfilePersonal();
            personal.setProfile(coreProfile);
            personal.setAttractions(new ArrayList<>());
            when(personalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(personal));

            PersonalProfileDTO dto = new PersonalProfileDTO(
                    "roxos", null, null, null, null, null, null,
                    null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updatePersonal(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for eyeColor");
        }

        @Test
        @DisplayName("Deve rejeitar bodyType inválido")
        void shouldRejectInvalidBodyType() {
            stubCoreProfile();
            UserProfilePersonal personal = new UserProfilePersonal();
            personal.setProfile(coreProfile);
            personal.setAttractions(new ArrayList<>());
            when(personalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(personal));

            PersonalProfileDTO dto = new PersonalProfileDTO(
                    null, null, null, "musculoso", null, null, null,
                    null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updatePersonal(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for bodyType");
        }

        @Test
        @DisplayName("Deve rejeitar education inválido no perfil profissional")
        void shouldRejectInvalidEducation() {
            stubCoreProfile();
            UserProfileProfessional prof = new UserProfileProfessional();
            prof.setProfile(coreProfile);
            when(professionalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(prof));

            ProfessionalProfileDTO dto = new ProfessionalProfileDTO(
                    "PhD em Hogwarts", null, null, null, null, null,
                    null, null, null, null, null, null, null);

            assertThatThrownBy(() -> profileService.updateProfessional(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid value for education");
        }

        @Test
        @DisplayName("Deve aceitar todos os valores válidos de relationship status")
        void shouldAcceptAllValidRelationshipStatuses() {
            stubCoreProfile();
            UserProfileGeneral general = createGeneralWithDefaults();
            when(generalRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(general));
            when(generalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            for (String status : com.orkutclone.api.validation.AllowedProfileValues.RELATIONSHIP_STATUS) {
                GeneralProfileDTO dto = new GeneralProfileDTO(
                        null, null, null, status,
                        null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null);
                GeneralProfileDTO result = profileService.updateGeneral(dto);
                assertThat(result.relationshipStatus()).isEqualTo(status);
            }
        }

        @Test
        @DisplayName("Deve aceitar valor null para campos com opções fixas (não altera)")
        void shouldAcceptNullForOptionFields() {
            stubCoreProfile();
            UserProfileSocial social = new UserProfileSocial();
            social.setProfile(coreProfile);
            social.setHumor(new ArrayList<>());
            social.setStyle(new ArrayList<>());
            social.setSmoking("não");
            when(socialRepository.findByProfileId(coreProfile.getId())).thenReturn(Optional.of(social));
            when(socialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SocialProfileDTO dto = new SocialProfileDTO(
                    null, null, null, null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null, null, null, null, null, null, null, null);

            SocialProfileDTO result = profileService.updateSocial(dto);
            assertThat(result.smoking()).isEqualTo("não");
        }
    }
}
