package com.orkutclone.api.service;

import com.orkutclone.api.dto.SearchResponse;
import com.orkutclone.api.repository.CommunityMembershipRepository;
import com.orkutclone.api.repository.ScrapRepository;
import com.orkutclone.api.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

/**
 * Pesquisa universal reutilizada por qualquer entrada de busca do sistema
 * (componente UniversalSearch do front-end).
 *
 * <p>Procura usuários (nome, e-mail, bio) e comunidades (nome, descrição) de forma
 * case-insensitive e com match parcial. O filtro {@code type} restringe quais
 * categorias são consultadas ("all", "users"/"usuarios", "communities"/"comunidades").
 * Os resultados vêm em uma lista unificada paginada, ordenada por relevância.</p>
 *
 * <p>Os filtros {@code location} e {@code language} são aceitos para compatibilidade
 * com o front-end, mas ainda não filtram: dependem de dados que hoje só existem no
 * perfil estendido (UserProfileGeneral) e serão ligados numa etapa futura. O tipo
 * "topics" também é aceito, porém não há modelo de tópico/fórum — retorna vazio.</p>
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int DEFAULT_SIZE = 12;
    private static final int MAX_SIZE = 50;

    private final SearchRepository searchRepository;
    private final ScrapRepository scrapRepository;
    private final CommunityMembershipRepository communityMembershipRepository;

    @Transactional(readOnly = true)
    public SearchResponse search(String query, String type, String location, String language, int page, int size) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) {
            throw new IllegalArgumentException("O parâmetro de busca 'q' é obrigatório.");
        }

        String normalizedType = type == null ? "all" : type.trim().toLowerCase();
        boolean includeUsers = matchesUsers(normalizedType);
        boolean includeCommunities = matchesCommunities(normalizedType);

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        // Cada palavra é um token; todas precisam aparecer no registro (em qualquer ordem).
        List<String> tokens = List.of(term.split("\\s+"));

        Page<Object[]> rows = searchRepository.search(tokens, term, includeUsers, includeCommunities, pageable);
        List<SearchResponse.SearchResult> results = rows.getContent().stream()
                .map(this::toResult)
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

    private SearchResponse.SearchResult toResult(Object[] row) {
        SearchResponse.ResultType resultType = SearchResponse.ResultType.valueOf((String) row[0]);
        UUID id = toUuid(row[1]);
        String name = (String) row[2];
        String avatarUrl = (String) row[3];
        String email = (String) row[4];
        String aboutMe = (String) row[5];

        long scrapCount = 0;
        long memberCount = 0;
        if (resultType == SearchResponse.ResultType.USER) {
            scrapCount = scrapRepository.countByOwnerId(id);
        } else {
            memberCount = communityMembershipRepository.countByCommunityId(id);
        }

        return new SearchResponse.SearchResult(resultType, id, name, avatarUrl, email, aboutMe, scrapCount, memberCount);
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
}
