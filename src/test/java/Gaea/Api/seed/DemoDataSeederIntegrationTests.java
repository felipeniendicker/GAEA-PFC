package Gaea.Api.seed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gaea-seed-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "app.demo-seed.enabled=true",
        "app.demo-seed.expected-database=gaea-seed-test",
        "app.demo-seed.password.instituicao=Senha-Instituicao-Teste-2026",
        "app.demo-seed.password.aluno=Senha-Aluno-Teste-2026",
        "app.demo-seed.password.supervisor=Senha-Supervisor-Teste-2026"
})
@ActiveProfiles("test")
class DemoDataSeederIntegrationTests {

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveCriarCargaCompletaEContinuarIdempotente() throws Exception {
        Contagens primeira = contagens();

        ApplicationArguments argumentos = new DefaultApplicationArguments(new String[0]);
        seeder.run(argumentos);

        assertThat(contagens()).isEqualTo(primeira);
        assertThat(primeira).isEqualTo(new Contagens(3, 148, 180, 60, 80));
        assertThat(contar("select count(*) from processo_estagio where status = 'PENDENTE'"))
                .isEqualTo(45);
        assertThat(contar("select count(*) from processo_estagio where status = 'EM_ANALISE'"))
                .isEqualTo(45);
        assertThat(contar("select count(*) from processo_estagio where status = 'APROVADO'"))
                .isEqualTo(45);
        assertThat(contar("select count(*) from processo_estagio where status = 'REPROVADO'"))
                .isEqualTo(45);
    }

    private Contagens contagens() {
        return new Contagens(
                contar("select count(*) from instituicao"),
                contar("select count(*) from usuario"),
                contar("select count(*) from processo_estagio"),
                contar("select count(*) from pendencia_processo"),
                contar("select count(*) from prazo_processo"));
    }

    private int contar(String sql) {
        Integer total = jdbc.queryForObject(sql, Integer.class);
        return total == null ? 0 : total;
    }

    private record Contagens(
            int instituicoes, int usuarios, int processos, int pendencias, int prazos) {
    }
}
