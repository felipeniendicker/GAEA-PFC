package Gaea.Api.service;

import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.TipoOperacaoStatus;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ProcessoEstagioTransactionalTests {

    @Autowired
    private ProcessoEstagioService service;

    @Autowired
    private ProcessoEstagioRepository processoRepository;

    @Autowired
    private HistoricoStatusProcessoRepository historicoRepository;

    @MockitoBean
    private LogAuditoriaService logAuditoriaService;

    @BeforeEach
    void limparBanco() {
        historicoRepository.deleteAll();
        processoRepository.deleteAll();
    }

    @Test
    void devePersistirHistoricoJuntoComAlteracaoDeStatus() {
        ProcessoEstagio processo = novoProcessoPendente();

        service.alterarStatus(
                processo.getId(),
                "EM_ANALISE",
                "instituicao@example.com");

        ProcessoEstagio processoAtualizado = processoRepository.findById(processo.getId()).orElseThrow();
        var historicos = historicoRepository.findByProcessoIdOrderByDataHoraAscIdAsc(processo.getId());

        assertEquals("EM_ANALISE", processoAtualizado.getStatus());
        assertEquals(1, historicos.size());
        assertEquals("PENDENTE", historicos.get(0).getStatusAnterior());
        assertEquals("EM_ANALISE", historicos.get(0).getStatusNovo());
        assertEquals("instituicao@example.com", historicos.get(0).getUsuarioResponsavel());
        assertEquals(TipoOperacaoStatus.ALTERACAO_NORMAL, historicos.get(0).getTipoOperacao());
    }

    @Test
    void deveDesfazerStatusEHistoricoQuandoAuditoriaFalhar() {
        ProcessoEstagio processo = novoProcessoPendente();

        Long processoId = processo.getId();
        doThrow(new RuntimeException("Falha simulada de auditoria"))
                .when(logAuditoriaService).registrar(anyString(), anyString(), anyString());

        assertThrows(
                RuntimeException.class,
                () -> service.alterarStatus(
                        processoId,
                        "EM_ANALISE",
                        "instituicao@example.com"));

        assertEquals("PENDENTE", processoRepository.findById(processoId).orElseThrow().getStatus());
        assertEquals(0, historicoRepository.count());
    }

    private ProcessoEstagio novoProcessoPendente() {
        ProcessoEstagio processo = new ProcessoEstagio();
        processo.setNomeAluno("Ana Souza");
        processo.setEmailAluno("ana@example.com");
        processo.setNomeEmpresa("Empresa Exemplo");
        processo.setCnpjEmpresa("11222333000181");
        processo.setStatus("PENDENTE");
        return processoRepository.save(processo);
    }
}
