package Gaea.Api.service;

import Gaea.Api.dto.DashboardResumoResponse;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
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

    @BeforeEach
    void limparBanco() {
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
    }

    @Test
    void deveRetornarResumoZeradoQuandoNaoExistiremProcessos() {
        DashboardResumoResponse resumo = dashboardService.obterResumo();

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

        DashboardResumoResponse resumo = dashboardService.obterResumo();

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

        DashboardResumoResponse resumo = dashboardService.obterResumo();

        assertEquals(3, resumo.totalProcessos());
        assertEquals(0, resumo.aprovados());
        assertEquals(0, resumo.reprovados());
        assertEquals(new BigDecimal("0.00"), resumo.percentualAprovacao());
    }

    @Test
    void deveArredondarPercentualDeAprovacaoParaDuasCasasDecimais() {
        salvarProcessos("APROVADO", 1);
        salvarProcessos("REPROVADO", 2);

        DashboardResumoResponse resumo = dashboardService.obterResumo();

        assertEquals(new BigDecimal("33.33"), resumo.percentualAprovacao());
    }

    private void salvarProcessos(String status, int quantidade) {
        for (int indice = 0; indice < quantidade; indice += 1) {
            ProcessoEstagio processo = new ProcessoEstagio();
            processo.setNomeAluno("Aluno " + status + " " + indice);
            processo.setEmailAluno("aluno" + status + indice + "@example.com");
            processo.setNomeEmpresa("Empresa de Teste");
            processo.setCnpjEmpresa("11222333000181");
            processo.setStatus(status);
            processoRepository.save(processo);
        }
    }
}
