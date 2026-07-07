package com.orkutclone.api.support;

import java.text.Normalizer;

/**
 * Utilitários de normalização de texto para a pesquisa.
 *
 * <p>{@link #unaccent(String)} remove acentos/diacríticos. No Postgres a busca usa a
 * função nativa {@code unaccent} (extensão {@code unaccent}); nos testes com H2, esta
 * mesma implementação é registrada como {@code ALIAS UNACCENT}, de modo que a query
 * nativa se comporte igual nos dois bancos.</p>
 */
public final class SearchText {

    private SearchText() {
    }

    public static String unaccent(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }
}
