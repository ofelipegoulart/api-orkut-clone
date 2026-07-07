package com.orkutclone.api.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Garante que a função {@code unaccent} usada pela pesquisa exista no banco em uso.
 *
 * <ul>
 *   <li><b>Postgres</b>: habilita a extensão {@code unaccent} (idempotente).</li>
 *   <li><b>H2</b> (testes): registra um {@code ALIAS UNACCENT} apontando para
 *       {@link com.orkutclone.api.support.SearchText#unaccent(String)}, para que a
 *       query nativa se comporte igual à do Postgres.</li>
 * </ul>
 *
 * <p>Se não for possível preparar a função (ex.: usuário sem permissão para criar a
 * extensão no Postgres), registra um aviso com a instrução manual em vez de impedir o
 * boot.</p>
 */
@Component
@RequiredArgsConstructor
public class SearchFunctionInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SearchFunctionInitializer.class);

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
            String ddl;
            if (product.contains("postgresql")) {
                ddl = "CREATE EXTENSION IF NOT EXISTS unaccent";
            } else if (product.contains("h2")) {
                ddl = "CREATE ALIAS IF NOT EXISTS UNACCENT FOR "
                        + "\"com.orkutclone.api.support.SearchText.unaccent\"";
            } else {
                log.warn("Banco '{}' não reconhecido: função 'unaccent' da pesquisa não foi preparada.", product);
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(ddl);
            }
            log.info("Função 'unaccent' pronta para a pesquisa ({}).", product);
        } catch (Exception e) {
            log.warn("Não foi possível preparar a função 'unaccent' para a pesquisa. "
                    + "No Postgres, rode manualmente 'CREATE EXTENSION unaccent;'. Causa: {}", e.getMessage());
        }
    }
}
