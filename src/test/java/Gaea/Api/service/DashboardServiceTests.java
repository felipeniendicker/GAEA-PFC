package Gaea.Api.service;

import Gaea.Api.dto.DashboardResumoResponse;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.InstituicaoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DashboardServiceTests {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private HistoricoStatusProcessoRepository historicoRepository;

    @Autowired
    private InstituicaoRepository instituicaoRepository;

    private Instituicao instituicao;
    private Usuario usuarioInstituicao;

    @BeforeEach
    void limparBanco() {
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
        instituicaoRepository.deleteAll();

        instituicao = novaInstituicao("Faculdade de Teste", "11222333000181");
        usuarioInstituicao = new Usuario();
        usuarioInstituicao.setPerfil("INSTITUICAO");
        usuarioInstituicao.setInstituicao(instituicao);
    }

    @Test
    void deveRetornarResumoZeradoQuandoNaoExistiremProcessos() {
        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(0, resumo.totalProcessos());
        assertEquals(0, resumo.pendentes());
        assertEquals(0, resumo.emAnalise());
        assertEquals(0, resumo.aprovados());
        assertEquals(0, resumo.reprovados());
        assertEquals(new BigDecimal("0.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveContarProcessosEmTodosOsStatusECalcularPercentual() {
        salvarProcessos("PENDENTE", 1);
        salvarProcessos("EM_ANALISE", 2);
        salvarProcessos("APROVADO", 3);
        salvarProcessos("REPROVADO", 1);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(7, resumo.totalProcessos());
        assertEquals(1, resumo.pendentes());
        assertEquals(2, resumo.emAnalise());
        assertEquals(3, resumo.aprovados());
        assertEquals(1, resumo.reprovados());
        assertEquals(new BigDecimal("75.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveRetornarPercentualZeroQuandoNaoExistiremDecisoesFinais() {
        salvarProcessos("PENDENTE", 2);
        salvarProcessos("EM_ANALISE", 1);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(3, resumo.totalProcessos());
        assertEquals(0, resumo.aprovados());
        assertEquals(0, resumo.reprovados());
        assertEquals(new BigDecimal("0.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveArredondarPercentualDeAprovacaoParaDuasCasasDecimais() {
        salvarProcessos("APROVADO", 1);
        salvarProcessos("REPROVADO", 2);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(new BigDecimal("33.33"), resumo.percentualAprovacao());
    }

    @Test
    void deveIgnorarProcessosDeOutraInstituicao() {
        salvarProcessos("PENDENTE", 1);
        Instituicao outraInstituicao = novaInstituicao(
                "Outra Faculdade",
                "22333444000160");
        salvarProcesso("APROVADO", outraInstituicao);

        DashboardResumoResponse resumo = dashboardService.obterResumo(usuarioInstituicao);

        assertEquals(1, resumo.totalProcessos());
        assertEquals(1, resumo.pendentes());
        assertEquals(0, resumo.aprovados());
    }

    private void salvarProcessos(String status, int quantidade) {
        for (int indice = 0; indice < quantidade; indice += 1) {
            salvarProcesso(status, instituicao);
        }
    }

    private void salvarProcesso(String status, Instituicao instituicaoDoProcesso) {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno("Aluno " + status);
        processo.setEmailAluno("aluno" + status + "@example.com");
        processo.setNomeEmpresa("Empresa de Teste");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus(status);
        processo.setInstituicao(instituicaoDoProcesso);
        processoRepository.save(processo);
    }

    private Instituicao novaInstituicao(String nome, String cnpj) {
        Instituicao novaInstituicao = new Instituicao();
        novaInstituicao.setNome(nome);
        novaInstituicao.setCnpj(cnpj);
        return instituicaoRepository.save(novaInstituicao);
    }
}
