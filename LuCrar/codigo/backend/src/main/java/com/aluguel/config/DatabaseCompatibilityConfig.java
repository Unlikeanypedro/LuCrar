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

            for (String table : new String[] { "usuario", "agente", "automovel" }) {
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
        };
    }
}
