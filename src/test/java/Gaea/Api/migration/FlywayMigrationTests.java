package Gaea.Api.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlywayMigrationTests {

    @Test
    void deveAplicarBaselineENovaEstruturaEmBancoDescartavel() throws Exception {
        String url = novaUrl();

        var resultado = flyway(url).migrate();

        assertEquals(5, resultado.migrationsExecuted);

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {

            try (var resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = 'public'
                      AND table_name IN (
                          'instituicao',
                          'vinculo_supervisor_processo',
                          'convite_ativacao_aluno'
                      )
                    """)) {
                resultSet.next();
                assertEquals(3, resultSet.getInt(1));
            }

            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO processo_estagio
                        (nome_aluno, email_aluno, nome_empresa, cnpj_empresa, status,
                         aluno_usuario_id, versao)
                    VALUES
                        ('Aluno', 'aluno@example.com', 'Empresa', '11222333000181',
                         'PENDENTE', 999999, 0)
                    """));

            statement.executeUpdate("""
                    INSERT INTO processo_estagio
                        (nome_aluno, email_aluno, nome_empresa, cnpj_empresa, status, versao)
                    VALUES
                        ('Aluno', 'aluno@example.com', 'Empresa', '11222333000181',
                         'PENDENTE', 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO vinculo_supervisor_processo
                        (processo_id, email_convidado, situacao, criado_em, ativo, versao)
                    SELECT id, 'supervisor@example.com', 'PENDENTE', CURRENT_TIMESTAMP, TRUE, 0
                    FROM processo_estagio
                    WHERE email_aluno = 'aluno@example.com'
                    """);
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO vinculo_supervisor_processo
                        (processo_id, email_convidado, situacao, criado_em, ativo, versao)
                    SELECT id, 'outro@example.com', 'PENDENTE', CURRENT_TIMESTAMP, TRUE, 0
                    FROM processo_estagio
                    WHERE email_aluno = 'aluno@example.com'
                    """));

            statement.executeUpdate("""
                    INSERT INTO vinculo_supervisor_processo
                        (processo_id, email_convidado, situacao, criado_em, ativo, versao)
                    SELECT id, 'recusado@example.com', 'RECUSADO', CURRENT_TIMESTAMP, NULL, 0
                    FROM processo_estagio
                    WHERE email_aluno = 'aluno@example.com'
                    """);
            statement.executeUpdate("""
                    INSERT INTO vinculo_supervisor_processo
                        (processo_id, email_convidado, situacao, criado_em, ativo, versao)
                    SELECT id, 'cancelado@example.com', 'CANCELADO', CURRENT_TIMESTAMP, NULL, 0
                    FROM processo_estagio
                    WHERE email_aluno = 'aluno@example.com'
                    """);
            try (var resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM vinculo_supervisor_processo v
                    JOIN processo_estagio p ON p.id = v.processo_id
                    WHERE p.email_aluno = 'aluno@example.com'
                    """)) {
                resultSet.next();
                assertEquals(3, resultSet.getInt(1));
            }
            statement.executeUpdate("""
                    INSERT INTO instituicao (nome, cnpj, ativa, criada_em)
                    VALUES
                        ('Faculdade A', '11222333000181', TRUE, CURRENT_TIMESTAMP),
                        ('Faculdade B', '22333444000160', TRUE, CURRENT_TIMESTAMP)
                    """);
            statement.executeUpdate("""
                    INSERT INTO usuario
                        (nome, email, senha, perfil, instituicao_id, matricula,
                         ativo, ativado, termos_aceitos)
                    SELECT 'Aluno A', 'aluno.a@example.com', 'hash', 'ALUNO', id,
                           '000123', TRUE, TRUE, TRUE
                    FROM instituicao WHERE cnpj = '11222333000181'
                    """);
            statement.executeUpdate("""
                    INSERT INTO usuario
                        (nome, email, senha, perfil, instituicao_id, matricula,
                         ativo, ativado, termos_aceitos)
                    SELECT 'Aluno B', 'aluno.b@example.com', 'hash', 'ALUNO', id,
                           '000123', TRUE, TRUE, TRUE
                    FROM instituicao WHERE cnpj = '22333444000160'
                    """);
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO usuario
                        (nome, email, senha, perfil, instituicao_id, matricula,
                         ativo, ativado, termos_aceitos)
                    SELECT 'Aluno Duplicado', 'duplicado@example.com', 'hash', 'ALUNO', id,
                           '000123', TRUE, TRUE, TRUE
                    FROM instituicao WHERE cnpj = '11222333000181'
                    """));
        }
    }

    @Test
    void devePreservarProcessoLegadoSemAssociacaoAutomatica() throws Exception {
        String url = novaUrl();

        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .target("1")
                .load()
                .migrate();

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO usuario
                        (nome, email, senha, perfil, termos_aceitos)
                    VALUES
                        ('Usuário Legado', 'usuario.legado@example.com',
                         'hash-existente', 'ALUNO', TRUE)
                    """);
            statement.executeUpdate("""
                    INSERT INTO processo_estagio
                        (nome_aluno, email_aluno, nome_empresa, cnpj_empresa, status)
                    VALUES
                        ('Aluno Legado', 'legado@example.com', 'Empresa Legada',
                         '11222333000181', 'PENDENTE')
                    """);
        }

        var resultado = flyway(url).migrate();
        assertEquals(4, resultado.migrationsExecuted);

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     SELECT nome_aluno, email_aluno, aluno_usuario_id, instituicao_id,
                            cep_empresa, endereco_empresa, versao
                     FROM processo_estagio
                     WHERE email_aluno = 'legado@example.com'
                     """)) {
            resultSet.next();
            assertEquals("Aluno Legado", resultSet.getString("nome_aluno"));
            assertEquals("legado@example.com", resultSet.getString("email_aluno"));
            assertNull(resultSet.getObject("aluno_usuario_id"));
            assertNull(resultSet.getObject("instituicao_id"));
            assertNull(resultSet.getObject("cep_empresa"));
            assertNull(resultSet.getObject("endereco_empresa"));
            assertEquals(0, resultSet.getLong("versao"));
        }

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("""
                     SELECT ativo, ativado, matricula, senha, instituicao_id, email_verificado_em
                     FROM usuario
                     WHERE email = 'usuario.legado@example.com'
                     """)) {
            resultSet.next();
            assertEquals(true, resultSet.getBoolean("ativo"));
            assertEquals(true, resultSet.getBoolean("ativado"));
            assertNull(resultSet.getObject("matricula"));
            assertEquals("hash-existente", resultSet.getString("senha"));
            assertNull(resultSet.getObject("instituicao_id"));
            assertNull(resultSet.getObject("email_verificado_em"));
        }
    }

    private Flyway flyway(String url) {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load();
    }

    private String novaUrl() {
        return "jdbc:h2:mem:flyway-" + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    }
}
