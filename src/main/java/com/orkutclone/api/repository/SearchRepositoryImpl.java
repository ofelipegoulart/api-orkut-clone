package com.orkutclone.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementação da pesquisa unificada com casamento por tokens e insensível a acentos.
 *
 * <p>A quantidade de tokens é variável, então a query nativa é montada dinamicamente:
 * para cada token adiciona-se um {@code unaccent(lower(haystack)) LIKE unaccent(lower(:pN))}
 * combinado por {@code AND}. Os valores dos tokens são sempre passados como parâmetros
 * (nunca concatenados), evitando SQL injection — só o número de cláusulas varia.</p>
 */
public class SearchRepositoryImpl implements SearchRepositoryCustom {

    // Texto pesquisável de cada entidade: nome + demais campos relevantes.
    private static final String USER_HAYSTACK =
            "coalesce(u.name,'') || ' ' || coalesce(u.email,'') || ' ' || coalesce(u.bio,'')";
    private static final String COMMUNITY_HAYSTACK =
            "coalesce(c.name,'') || ' ' || coalesce(c.description,'')";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Object[]> search(List<String> tokens,
                                 String fullTerm,
                                 boolean includeUsers,
                                 boolean includeCommunities,
                                 Pageable pageable) {

        if ((!includeUsers && !includeCommunities) || tokens.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        List<String> unionBranches = new ArrayList<>();
        if (includeUsers) {
            unionBranches.add("""
                    SELECT 'USER' AS result_type,
                           u.id AS id,
                           u.name AS name,
                           u.profile_picture AS avatar_url,
                           u.email AS email,
                           u.bio AS about_me,
                           CASE
                               WHEN unaccent(lower(u.name)) = unaccent(lower(:fullTerm)) THEN 0
                               WHEN unaccent(lower(u.name)) LIKE unaccent(lower(:fullPrefix)) THEN 1
                               ELSE 2
                           END AS relevance
                    FROM users u
                    WHERE %s
                    """.formatted(tokenPredicate(USER_HAYSTACK, tokens.size())));
        }
        if (includeCommunities) {
            unionBranches.add("""
                    SELECT 'COMMUNITY' AS result_type,
                           c.id AS id,
                           c.name AS name,
                           c.icon AS avatar_url,
                           NULL AS email,
                           c.description AS about_me,
                           CASE
                               WHEN unaccent(lower(c.name)) = unaccent(lower(:fullTerm)) THEN 0
                               WHEN unaccent(lower(c.name)) LIKE unaccent(lower(:fullPrefix)) THEN 1
                               ELSE 2
                           END AS relevance
                    FROM communities c
                    WHERE %s
                    """.formatted(tokenPredicate(COMMUNITY_HAYSTACK, tokens.size())));
        }

        String union = String.join(" UNION ALL ", unionBranches);

        String dataSql = """
                SELECT sub.result_type, sub.id, sub.name, sub.avatar_url, sub.email, sub.about_me
                FROM ( %s ) sub
                ORDER BY sub.relevance ASC, sub.name ASC
                """.formatted(union);
        String countSql = "SELECT count(*) FROM ( %s ) sub".formatted(union);

        Query dataQuery = entityManager.createNativeQuery(dataSql);
        Query countQuery = entityManager.createNativeQuery(countSql);
        bindParameters(dataQuery, tokens, fullTerm);
        bindParameters(countQuery, tokens, fullTerm);

        dataQuery.setFirstResult((int) pageable.getOffset());
        dataQuery.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(rows, pageable, total);
    }

    /** Monta "unaccent(lower(haystack)) LIKE unaccent(lower(:p0)) AND ... :pN". */
    private String tokenPredicate(String haystack, int tokenCount) {
        List<String> clauses = new ArrayList<>(tokenCount);
        for (int i = 0; i < tokenCount; i++) {
            clauses.add("unaccent(lower(%s)) LIKE unaccent(lower(:p%d))".formatted(haystack, i));
        }
        return String.join(" AND ", clauses);
    }

    private void bindParameters(Query query, List<String> tokens, String fullTerm) {
        query.setParameter("fullTerm", fullTerm);
        query.setParameter("fullPrefix", fullTerm + "%");
        for (int i = 0; i < tokens.size(); i++) {
            query.setParameter("p" + i, "%" + tokens.get(i) + "%");
        }
    }
}
