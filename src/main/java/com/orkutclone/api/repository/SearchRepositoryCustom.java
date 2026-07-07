package com.orkutclone.api.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Consulta unificada da pesquisa universal (implementada em {@link SearchRepositoryImpl}).
 *
 * <p>Une usuários e comunidades em uma única lista paginada no banco. O casamento é por
 * <em>tokens</em>: cada palavra de {@code tokens} precisa aparecer no registro (em qualquer
 * ordem), ignorando acentos — assim "rafael mauricio", "Maurício Rafael" e
 * "rafael silva mauricio" encontram o mesmo perfil.</p>
 *
 * <p>Cada linha volta como {@code Object[]} na ordem:
 * {@code [0]=result_type, [1]=id, [2]=name, [3]=avatar_url, [4]=email, [5]=about_me}.</p>
 */
public interface SearchRepositoryCustom {

    Page<Object[]> search(List<String> tokens,
                          String fullTerm,
                          boolean includeUsers,
                          boolean includeCommunities,
                          Pageable pageable);
}
