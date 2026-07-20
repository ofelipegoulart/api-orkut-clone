package com.orkutclone.api.service;

import com.orkutclone.api.dto.SearchResponse;
import com.orkutclone.api.model.Community;
import com.orkutclone.api.model.CommunityTopic;
import com.orkutclone.api.model.TopicMessage;
import com.orkutclone.api.model.UserProfileGeneral;
import com.orkutclone.api.model.UserProfileSocial;
import com.orkutclone.api.repository.CommunityMembershipRepository;
import com.orkutclone.api.repository.CommunityRepository;
import com.orkutclone.api.repository.CommunityTopicRepository;
import com.orkutclone.api.repository.ScrapRepository;
import com.orkutclone.api.repository.SearchRepository;
import com.orkutclone.api.repository.TopicMessageRepository;
import com.orkutclone.api.repository.UserProfileGeneralRepository;
import com.orkutclone.api.repository.UserProfileSocialRepository;
import com.orkutclone.api.support.SearchText;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pesquisa universal reutilizada por qualquer entrada de busca do sistema
 * (componente UniversalSearch do front-end).
 *
 * <p>Procura usuários (nome, e-mail, bio), comunidades (nome, descrição) e tópicos de fórum
 * (título ou conteúdo de qualquer mensagem) de forma case-insensitive e com match parcial. O
 * filtro {@code type} restringe quais categorias são consultadas ("all", "users"/"usuarios",
 * "communities"/"comunidades", "topics"/"topicos"). Os resultados vêm em uma lista unificada
 * paginada, ordenada por relevância.</p>
 *
 * <p>O filtro {@code location} é aceito para compatibilidade com o front-end, mas ainda não
 * filtra: depende de dados que hoje só existem no perfil estendido (UserProfileGeneral) e será
 * ligado numa etapa futura.</p>
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;

    private final SearchRepository searchRepository;
    private final ScrapRepository scrapRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMembershipRepository communityMembershipRepository;
    private final CommunityTopicRepository communityTopicRepository;
    private final TopicMessageRepository topicMessageRepository;
    private final UserProfileGeneralRepository userProfileGeneralRepository;
    private final UserProfileSocialRepository userProfileSocialRepository;

    @Transactional(readOnly = true)
    public SearchResponse search(String query, String type, String location, String language, int page, int size) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            throw new IllegalArgumentException("O parâmetro de busca 'q' é obrigatório.");
        }

        String normalizedType = type == null ? "all" : type.trim().toLowerCase();
        boolean includeUsers = matchesUsers(normalizedType);
        boolean includeCommunities = matchesCommunities(normalizedType);
        boolean includeTopics = matchesTopics(normalizedType);

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        // Cada palavra é um token; todas precisam aparecer no registro (em qualquer ordem).
        List<String> tokens = List.of(term.split("\\s+"));

        Page<Object[]> rows = searchRepository.search(tokens, term, includeUsers, includeCommunities, includeTopics, pageable);
        List<SearchResponse.SearchResult> results = rows.getContent().stream()
                .map(row -> toResult(row, tokens))
                .toList();

        return new SearchResponse(
                term,
                normalizedType,
                safePage,
                safeSize,
                rows.getTotalElements(),
                rows.getTotalPages(),
                results);
    }

    private SearchResponse.SearchResult toResult(Object[] row, List<String> tokens) {
        SearchResponse.ResultType resultType = SearchResponse.ResultType.valueOf((String) row[0]);
        UUID id = toUuid(row[1]);
        String name = (String) row[2];
        String avatarUrl = (String) row[3];
        String email = (String) row[4];
        String aboutMe = (String) row[5];

        long scrapCount = 0;
        long memberCount = 0;
        long messageCount = 0;
        String city = null;
        String country = null;
        String category = null;
        UUID communityId = null;
        String communityName = null;
        java.time.OffsetDateTime lastMessageAt = null;

        switch (resultType) {
            case USER -> {
                scrapCount = scrapRepository.countByOwnerId(id);
                Optional<UserProfileGeneral> general = userProfileGeneralRepository.findByProfileUserId(id);
                city = general.map(UserProfileGeneral::getCity).orElse(null);
                country = general.map(UserProfileGeneral::getCountry).orElse(null);
                aboutMe = userProfileSocialRepository.findByProfileUserId(id)
                        .map(UserProfileSocial::getAboutMe)
                        .orElse(null);
            }
            case COMMUNITY -> {
                memberCount = communityMembershipRepository.countByCommunityId(id);
                Community community = communityRepository.findById(id).orElse(null);
                if (community != null) {
                    category = community.getCategory() == null ? null : community.getCategory().name();
                    if (community.getLocation() != null) {
                        city = community.getLocation().getCity();
                        country = community.getLocation().getCountry();
                    }
                }
            }
            case TOPIC -> {
                CommunityTopic topic = communityTopicRepository.findByIdWithCommunity(id).orElse(null);
                if (topic != null) {
                    communityId = topic.getCommunity().getId();
                    communityName = topic.getCommunity().getName();
                }
                messageCount = topicMessageRepository.countByTopicId(id);
                lastMessageAt = topicMessageRepository.findFirstByTopicIdOrderByCreatedAtDesc(id)
                        .map(m -> m.getCreatedAt().atOffset(ZoneOffset.UTC))
                        .orElse(null);
                aboutMe = findMatchingSnippet(id, tokens, name);
            }
        }

        return new SearchResponse.SearchResult(
                resultType, id, name, avatarUrl, email, aboutMe,
                scrapCount, memberCount, city, country, category,
                communityId, communityName, messageCount, lastMessageAt);
    }

    /** Devolve o texto da primeira mensagem do tópico que casa com os tokens da busca, ou o título como fallback. */
    private String findMatchingSnippet(UUID topicId, List<String> tokens, String topicTitle) {
        List<TopicMessage> messages = topicMessageRepository.findByTopicIdOrderByCreatedAtAsc(topicId);
        for (TopicMessage message : messages) {
            String haystack = SearchText.unaccent((message.getSubject() + " " + message.getMessage()).toLowerCase());
            boolean matchesAll = tokens.stream()
                    .allMatch(token -> haystack.contains(SearchText.unaccent(token.toLowerCase())));
            if (matchesAll) {
                return message.getMessage();
            }
        }
        return topicTitle;
    }

    /**
     * Converte a coluna id da query nativa em {@link UUID}. O driver Postgres devolve
     * {@code java.util.UUID}, mas o H2 (usado nos testes) devolve {@code byte[16]}.
     */
    private UUID toUuid(Object value) {
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String text) {
            return UUID.fromString(text);
        }
        if (value instanceof byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }
        throw new IllegalStateException("Tipo de id inesperado na pesquisa: " + value.getClass());
    }

    private boolean matchesUsers(String type) {
        return type.equals("all") || type.equals("users") || type.equals("usuarios") || type.equals("usuários");
    }

    private boolean matchesCommunities(String type) {
        return type.equals("all") || type.equals("communities") || type.equals("comunidades");
    }

    private boolean matchesTopics(String type) {
        return type.equals("all") || type.equals("topics") || type.equals("topicos") || type.equals("tópicos");
    }
}
