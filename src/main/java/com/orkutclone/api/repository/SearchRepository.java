package com.orkutclone.api.repository;

import com.orkutclone.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Ponto de entrada da pesquisa universal. A lógica fica em
 * {@link SearchRepositoryCustom}/{@link SearchRepositoryImpl}, que monta a query
 * nativa de UNION por tokens e insensível a acentos.
 */
public interface SearchRepository extends JpaRepository<User, UUID>, SearchRepositoryCustom {
}
