package Gaea.Api.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.flyway.clean-disabled=true",
        "spring.flyway.validate-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false"
})
@Testcontainers(disabledWithoutDocker = true)
class MySqlFlywayIntegrationTests {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.44")
            .withDatabaseName("gaea_migration_test")
            .withUsername("gaea_test")
            .withPassword("gaea_test");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.url", MYSQL::getJdbcUrl);
        registry.add("spring.flyway.user", MYSQL::getUsername);
        registry.add("spring.flyway.password", MYSQL::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void deveAplicarV1AV17EValidarMapeamentoJpaNoMySqlReal() {
        Integer migracoesAplicadas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version IS NOT NULL
                  AND success = 1
                """, Integer.class);
        Integer tabelasDeNegocio = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name <> 'flyway_schema_history'
                """, Integer.class);

        assertEquals(17, migracoesAplicadas);
        assertEquals(20, tabelasDeNegocio);

        Integer colunaSigla = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'instituicao'
                  AND column_name = 'sigla'
                  AND is_nullable = 'YES'
                  AND character_maximum_length = 20
                """, Integer.class);
        Integer colunaFicticia = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'instituicao'
                  AND column_name = 'ficticia'
                  AND is_nullable = 'NO'
                  AND column_default = '0'
                """, Integer.class);

        assertEquals(1, colunaSigla);
        assertEquals(1, colunaFicticia);

        Integer colunasAceiteLegalV17 = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'usuario'
                  AND column_name IN (
                      'termos_uso_versao', 'politica_privacidade_versao'
                  )
                  AND is_nullable = 'YES'
                  AND character_maximum_length = 30
                """, Integer.class);

        assertEquals(2, colunasAceiteLegalV17);

        Integer colunasPrivacidadeV16 = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'solicitacao_privacidade'
                  AND column_name IN (
                      'atualizado_em', 'responsavel_usuario_id', 'decisao',
                      'evidencia_execucao', 'versao'
                  )
                """, Integer.class);
        Integer constraintsPrivacidadeV16 = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE()
                  AND table_name = 'solicitacao_privacidade'
                  AND constraint_name IN (
                      'ck_solicitacao_privacidade_tipo',
                      'ck_solicitacao_privacidade_situacao',
                      'fk_solicitacao_privacidade_responsavel'
                  )
                """, Integer.class);
        Integer indicesPrivacidadeV16 = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'solicitacao_privacidade'
                  AND index_name IN (
                      'idx_solicitacao_privacidade_situacao_data',
                      'idx_solicitacao_privacidade_responsavel'
                  )
                """, Integer.class);

        assertEquals(5, colunasPrivacidadeV16);
        assertEquals(3, constraintsPrivacidadeV16);
        assertEquals(2, indicesPrivacidadeV16);
    }
}
