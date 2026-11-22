package Gaea.Api.seed;

import Gaea.Api.storage.ArmazenamentoDocumento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gaea-professional-demo-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "app.documentos.storage.tipo=memory-test",
        "app.professional-demo-seed.enabled=true",
        "app.professional-demo-seed.expected-database=gaea-professional-demo-test",
        "app.professional-demo-seed.expected-environment=local-demo",
        "app.professional-demo-seed.confirmation=GAEA_PROFESSIONAL_DEMO_ONLY",
        "app.professional-demo-seed.reference-date=2026-11-21",
        "app.professional-demo-seed.password.felipe=Felipe-Demo-Segura-2026!",
        "app.professional-demo-seed.password.aluno=Aluno-Demo-Segura-2026!",
        "app.professional-demo-seed.password.instituicao=Instituicao-Demo-Segura-2026!",
        "app.professional-demo-seed.password.supervisor=Supervisor-Demo-Segura-2026!",
        "app.email.enabled=false"
})
@ActiveProfiles("demo")
@Import(ProfessionalDemoSeederIntegrationTests.ArmazenamentoEmMemoriaConfig.class)
class ProfessionalDemoSeederIntegrationTests {

    private static final String DOMINIO_RESERVADO = "%@professional-demo.gaea.example";

    @Autowired
    private ProfessionalDemoSeeder seeder;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ArmazenamentoEmMemoria armazenamento;

    @Test
    void deveCriarCargaProfissionalCompletaEIdempotente() {
        Contagens primeiraExecucao = contagens();
        ApplicationArguments argumentos = new DefaultApplicationArguments(new String[0]);

        seeder.run(argumentos);

        assertThat(contagens()).isEqualTo(primeiraExecucao);
        assertThat(primeiraExecucao).isEqualTo(new Contagens(
                3, 42, 36, 18, 24, 30, 90, 90, 24, 72, 6));
        assertThat(armazenamento.quantidade()).isEqualTo(24);

        assertThat(contar("select count(*) from processo_estagio where status = 'PENDENTE'"))
                .isEqualTo(9);
        assertThat(contar("select count(*) from processo_estagio where status = 'EM_ANALISE'"))
                .isEqualTo(9);
        assertThat(contar("select count(*) from processo_estagio where status = 'APROVADO'"))
                .isEqualTo(9);
        assertThat(contar("select count(*) from processo_estagio where status = 'REPROVADO'"))
                .isEqualTo(9);
        assertThat(contar("select count(*) from vinculo_supervisor_processo where situacao = 'PENDENTE'"))
                .isEqualTo(6);
        assertThat(contar("select count(*) from vinculo_supervisor_processo where situacao = 'CONFIRMADO'"))
                .isEqualTo(30);
        assertThat(contar("select count(*) from historico_status_processo"))
                .isEqualTo(51);
        assertThat(contar("select count(*) from historico_vinculo_supervisor"))
                .isEqualTo(66);
        assertThat(armazenamento.todosSaoPdfFicticio()).isTrue();
    }

    @Test
    void deveCriarFelipeNaUmcComAcessoEContextoAcademico() {
        Map<String, Object> felipe = jdbc.queryForMap("""
                select u.nome, u.email, u.senha, u.perfil, u.matricula, i.nome as instituicao
                from usuario u
                join instituicao i on i.id = u.instituicao_id
                where u.email = 'felipe.rafael.niendicker@professional-demo.gaea.example'
                """);

        assertThat(felipe.get("nome")).isEqualTo("Felipe Rafael Niendicker");
        assertThat(felipe.get("perfil")).isEqualTo("ALUNO");
        assertThat(felipe.get("matricula")).isEqualTo("UMC-SI-2026-001");
        assertThat(felipe.get("instituicao"))
                .isEqualTo("Universidade de Mogi das Cruzes (UMC)");
        assertThat(passwordEncoder.matches(
                "Felipe-Demo-Segura-2026!", felipe.get("senha").toString())).isTrue();
        assertThat(contar("""
                select count(*) from log_auditoria
                where usuario = 'felipe.rafael.niendicker@professional-demo.gaea.example'
                  and recurso like '%Sistemas de Informação%'
                """)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void deveUsarSomenteIdentidadesNaturaisEEmaisReservados() {
        assertThat(contar("select count(*) from usuario where email not like ?", DOMINIO_RESERVADO))
                .isZero();
        assertThat(contar("select count(*) from usuario where lower(nome) like '%teste%' or lower(nome) like '%demo%'"))
                .isZero();
        assertThat(contar("select count(*) from instituicao where lower(nome) like '%teste%' or lower(nome) like '%demo%'"))
                .isZero();
        assertThat(contar("select count(*) from processo_estagio where lower(nome_empresa) like '%teste%' or lower(nome_empresa) like '%demo%'"))
                .isZero();
        assertThat(contar("select count(distinct nome_empresa) from processo_estagio"))
                .isEqualTo(8);
    }

    @Test
    void deveRecusarAtivacaoSemTodasAsTravas() {
        assertThatThrownBy(() -> ProfessionalDemoSeeder.validarAutorizacao(
                false, "demo", "demo", ProfessionalDemoSeeder.CONFIRMACAO_EXIGIDA, false))
                .hasMessageContaining("perfil demo");
        assertThatThrownBy(() -> ProfessionalDemoSeeder.validarAutorizacao(
                true, "production", "demo", ProfessionalDemoSeeder.CONFIRMACAO_EXIGIDA, false))
                .hasMessageContaining("ambiente");
        assertThatThrownBy(() -> ProfessionalDemoSeeder.validarAutorizacao(
                true, "demo", "demo", "confirmacao-incorreta", false))
                .hasMessageContaining("confirmação");
        assertThatThrownBy(() -> ProfessionalDemoSeeder.validarAutorizacao(
                true, "demo", "demo", ProfessionalDemoSeeder.CONFIRMACAO_EXIGIDA, true))
                .hasMessageContaining("e-mail");
    }

    private Contagens contagens() {
        return new Contagens(
                contar("select count(*) from instituicao"),
                contar("select count(*) from usuario"),
                contar("select count(*) from processo_estagio"),
                contar("select count(*) from pendencia_processo"),
                contar("select count(*) from prazo_processo"),
                contar("select count(*) from plano_atividades"),
                contar("select count(*) from atividade_plano"),
                contar("select count(*) from registro_carga_horaria"),
                contar("select count(*) from documento_processo"),
                contar("select count(*) from notificacao"),
                contar("select count(*) from solicitacao_privacidade"));
    }

    private int contar(String sql, Object... parametros) {
        Integer total = jdbc.queryForObject(sql, Integer.class, parametros);
        return total == null ? 0 : total;
    }

    private record Contagens(
            int instituicoes,
            int usuarios,
            int processos,
            int pendencias,
            int prazos,
            int planos,
            int atividades,
            int cargasHorarias,
            int documentos,
            int notificacoes,
            int solicitacoesPrivacidade) {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ArmazenamentoEmMemoriaConfig {

        @Bean
        @Primary
        ArmazenamentoEmMemoria armazenamentoEmMemoria() {
            return new ArmazenamentoEmMemoria();
        }
    }

    static class ArmazenamentoEmMemoria implements ArmazenamentoDocumento {

        private final Map<String, byte[]> documentos = new ConcurrentHashMap<>();

        @Override
        public void armazenar(String chave, byte[] conteudo) {
            documentos.put(chave, conteudo.clone());
        }

        @Override
        public Resource carregar(String chave) {
            byte[] conteudo = documentos.get(chave);
            return conteudo == null ? null : new ByteArrayResource(conteudo);
        }

        @Override
        public void remover(String chave) {
            documentos.remove(chave);
        }

        int quantidade() {
            return documentos.size();
        }

        boolean todosSaoPdfFicticio() {
            byte[] cabecalhoPdf = "%PDF-".getBytes();
            return documentos.values().stream().allMatch(conteudo ->
                    conteudo.length > cabecalhoPdf.length
                            && java.util.Arrays.equals(
                            cabecalhoPdf,
                            java.util.Arrays.copyOf(conteudo, cabecalhoPdf.length)));
        }
    }
}
