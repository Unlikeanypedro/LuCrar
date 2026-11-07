package com.aluguel.config;

import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.Ordered;

/** Re-synchronizes identity sequences after data imported with explicit IDs. */
@Configuration
public class DatabaseCompatibilityConfig {

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    CommandLineRunner alignImportedSequences(DataSource dataSource, JdbcTemplate jdbc) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")) {
                    return;
                }
            }

            for (String table : new String[] { "usuario", "agente", "automovel", "contrato_aluguel", "contrato_credito" }) {
                String sequence = jdbc.queryForObject(
                    "SELECT pg_get_serial_sequence(?, 'id')",
                    String.class,
                    "public." + table
                );
                if (sequence != null) {
                    jdbc.queryForObject(
                        "SELECT setval(CAST(? AS regclass), COALESCE(MAX(id), 1), MAX(id) IS NOT NULL) FROM public." + table,
                        Long.class,
                        sequence
                    );
                }
            }
            alignCreditoBankForeignKey(jdbc);
        };
    }

    private void alignCreditoBankForeignKey(JdbcTemplate jdbc) {
        Boolean tableExists = jdbc.queryForObject(
            "SELECT to_regclass('public.contrato_credito') IS NOT NULL",
            Boolean.class
        );
        if (!Boolean.TRUE.equals(tableExists)) return;

        boolean referencesAgent = referencesBankTable(jdbc, "agente");
        boolean referencesUser = referencesBankTable(jdbc, "usuario");
        if (referencesAgent && !referencesUser) return;

        // Previous versions linked the credit to usuario.id. Re-key those rows
        // to agente.id before enforcing the deployed schema's bank-agent link.
        if (referencesUser) {
            jdbc.update("""
                UPDATE public.contrato_credito credito
                SET banco_id = agente.id
                FROM public.agente agente
                WHERE credito.banco_id = agente.usuario_id
                  AND credito.banco_id <> agente.id
                """);
        }

        jdbc.execute("""
            DO $$
            DECLARE restricao RECORD;
            BEGIN
                FOR restricao IN
                    SELECT DISTINCT c.conname
                    FROM pg_constraint c
                    JOIN pg_class tabela ON tabela.oid = c.conrelid
                    JOIN pg_namespace esquema ON esquema.oid = tabela.relnamespace
                    JOIN unnest(c.conkey) coluna(attnum) ON TRUE
                    JOIN pg_attribute atributo
                      ON atributo.attrelid = tabela.oid
                     AND atributo.attnum = coluna.attnum
                    WHERE c.contype = 'f'
                      AND esquema.nspname = 'public'
                      AND tabela.relname = 'contrato_credito'
                      AND atributo.attname = 'banco_id'
                LOOP
                    EXECUTE format(
                        'ALTER TABLE public.contrato_credito DROP CONSTRAINT %I',
                        restricao.conname
                    );
                END LOOP;
                ALTER TABLE public.contrato_credito
                    ADD CONSTRAINT fk_contrato_credito_agente
                    FOREIGN KEY (banco_id) REFERENCES public.agente(id);
            END $$
            """);
    }

    private boolean referencesBankTable(JdbcTemplate jdbc, String targetTable) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS (
                SELECT 1
                FROM pg_constraint c
                JOIN pg_class tabela ON tabela.oid = c.conrelid
                JOIN pg_namespace esquema ON esquema.oid = tabela.relnamespace
                JOIN pg_class alvo ON alvo.oid = c.confrelid
                JOIN pg_namespace esquema_alvo ON esquema_alvo.oid = alvo.relnamespace
                JOIN unnest(c.conkey) coluna(attnum) ON TRUE
                JOIN pg_attribute atributo
                  ON atributo.attrelid = tabela.oid
                 AND atributo.attnum = coluna.attnum
                WHERE c.contype = 'f'
                  AND esquema.nspname = 'public'
                  AND tabela.relname = 'contrato_credito'
                  AND atributo.attname = 'banco_id'
                  AND esquema_alvo.nspname = 'public'
                  AND alvo.relname = ?
            )
            """, Boolean.class, targetTable));
    }
}
