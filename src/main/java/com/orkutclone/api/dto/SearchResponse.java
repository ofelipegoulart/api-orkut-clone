package com.orkutclone.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Resposta da pesquisa universal (tela de resultados do UniversalSearch).
 *
 * <p>Os resultados vêm em uma única lista {@code results}, cada item marcado com
 * {@link ResultType} indicando a origem (usuário ou comunidade). O envelope carrega
 * os metadados de paginação para o front-end montar "Resultados X-Y de Z".</p>
 */
public record SearchResponse(
        String query,
        String type,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<SearchResult> results
) {

    public enum ResultType { USER, COMMUNITY }

    public record SearchResult(
            ResultType resultType,
            UUID id,
            String name,
            @Schema(nullable = true) String avatarUrl,
            @Schema(nullable = true, description = "Preenchido apenas para USER") String email,
            @Schema(nullable = true, description = "Bio do usuário ou descrição da comunidade") String aboutMe,
            @Schema(description = "Total de scraps recebidos (USER); 0 para comunidades") long scrapCount,
            @Schema(description = "Total de membros (COMMUNITY); 0 para usuários") long memberCount
    ) {}
}
