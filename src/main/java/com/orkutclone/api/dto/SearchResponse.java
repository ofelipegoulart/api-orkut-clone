package com.orkutclone.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Resposta da pesquisa universal (tela de resultados do UniversalSearch).
 *
 * <p>Os resultados vêm em uma única lista {@code results}, cada item marcado com
 * {@link ResultType} indicando a origem (usuário, comunidade ou tópico de fórum). O envelope
 * carrega os metadados de paginação para o front-end montar "Resultados X-Y de Z".</p>
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

    public enum ResultType { USER, COMMUNITY, TOPIC }

    public record SearchResult(
            ResultType resultType,
            UUID id,
            String name,
            @Schema(nullable = true) String avatarUrl,
            @Schema(nullable = true, description = "Preenchido apenas para USER") String email,
            @Schema(nullable = true, description = "USER: campo \"quem eu sou\" do perfil social. "
                    + "COMMUNITY: descrição da comunidade. TOPIC: trecho da mensagem (ou o título) onde o termo foi encontrado.")
            String aboutMe,
            @Schema(description = "Total de scraps recebidos (USER); 0 para os demais tipos") long scrapCount,
            @Schema(description = "Total de membros (COMMUNITY); 0 para os demais tipos") long memberCount,
            @Schema(nullable = true, description = "Cidade (USER: do perfil geral; COMMUNITY: da localização)") String city,
            @Schema(nullable = true, description = "País (USER: do perfil geral; COMMUNITY: da localização)") String country,
            @Schema(nullable = true, description = "Preenchido apenas para COMMUNITY (nome do enum CommunityCategory)") String category,
            @Schema(nullable = true, description = "TOPIC: id da comunidade em que o tópico está") UUID communityId,
            @Schema(nullable = true, description = "TOPIC: nome da comunidade em que o tópico está") String communityName,
            @Schema(description = "TOPIC: total de mensagens no tópico; 0 para os demais tipos") long messageCount,
            @Schema(nullable = true, description = "TOPIC: data/hora da última mensagem postada") OffsetDateTime lastMessageAt
    ) {}
}
