package Gaea.Api.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationSqlOrderTests {

    @Test
    void deveCriarIndiceDaChaveEstrangeiraAntesDeRemoverIndiceUnico() throws Exception {
        String v2 = lerMigracao("V2__adiciona_instituicoes_e_vinculos.sql");
        String v5 = lerMigracao("V5__verifica_email_e_historico_supervisao.sql");

        assertTrue(v2.contains(
                "CONSTRAINT uk_vinculo_supervisor_processo UNIQUE (processo_id)"));
        assertTrue(v2.contains(
                "CONSTRAINT fk_vinculo_supervisor_processo\n"
                        + "        FOREIGN KEY (processo_id) REFERENCES processo_estagio (id)"));

        int criaIndiceDaChaveEstrangeira = v5.indexOf(
                "CREATE INDEX idx_vinculo_supervisor_processo_fk\n"
                        + "    ON vinculo_supervisor_processo (processo_id)");
        int removeIndiceUnicoAntigo = v5.indexOf(
                "DROP INDEX uk_vinculo_supervisor_processo");
        int criaUnicidadeDoVinculoAtivo = v5.indexOf(
                "ADD CONSTRAINT uk_vinculo_supervisor_ativo "
                        + "UNIQUE (processo_id, ativo)");

        assertTrue(criaIndiceDaChaveEstrangeira >= 0,
                "A V5 deve criar um índice dedicado para a chave estrangeira");
        assertTrue(removeIndiceUnicoAntigo > criaIndiceDaChaveEstrangeira,
                "O índice da chave estrangeira deve existir antes da remoção do índice único");
        assertTrue(criaUnicidadeDoVinculoAtivo > removeIndiceUnicoAntigo,
                "A nova regra de unicidade deve ser criada depois da remoção da regra antiga");
    }

    @Test
    void devePreservarMultiplosVinculosHistoricosEUmUnicoAtivo() throws Exception {
        String v5 = lerMigracao("V5__verifica_email_e_historico_supervisao.sql");

        assertTrue(v5.contains("ADD COLUMN ativo BOOLEAN NULL"));
        assertTrue(v5.contains(
                "WHEN situacao IN ('PENDENTE', 'CONFIRMADO') THEN TRUE\n"
                        + "    ELSE NULL"));
        assertTrue(v5.contains(
                "UNIQUE (processo_id, ativo)"));
        assertTrue(v5.contains(
                "FOREIGN KEY (vinculo_id) REFERENCES vinculo_supervisor_processo (id) "
                        + "ON DELETE RESTRICT"));
    }

    private String lerMigracao(String nome) throws IOException {
        String caminho = "db/migration/" + nome;
        try (var conteudo = getClass().getClassLoader().getResourceAsStream(caminho)) {
            if (conteudo == null) {
                throw new IOException("Migração não encontrada: " + caminho);
            }
            return new String(conteudo.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n");
        }
    }
}
