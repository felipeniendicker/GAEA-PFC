package Gaea.Api.service;

import Gaea.Api.dto.HistoricoProcessoResponse;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.HistoricoStatusProcesso;
import Gaea.Api.model.HistoricoVinculoSupervisor;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.LogAuditoria;
import Gaea.Api.model.PendenciaProcesso;
import Gaea.Api.model.PrazoProcesso;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.TipoOperacaoStatus;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.HistoricoStatusProcessoRepository;
import Gaea.Api.repository.HistoricoVinculoSupervisorRepository;
import Gaea.Api.repository.LogAuditoriaRepository;
import Gaea.Api.repository.PendenciaProcessoRepository;
import Gaea.Api.repository.PrazoProcessoRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoProcessoServiceTests {

    @Mock private ProcessoEstagioRepository processoRepository;
    @Mock private ProcessoAutorizacaoService autorizacaoService;
    @Mock private HistoricoStatusProcessoRepository historicoStatusRepository;
    @Mock private HistoricoVinculoSupervisorRepository historicoVinculoRepository;
    @Mock private PendenciaProcessoRepository pendenciaRepository;
    @Mock private PrazoProcessoRepository prazoRepository;
    @Mock private LogAuditoriaRepository auditoriaRepository;

    private HistoricoProcessoService service;
    private ProcessoEstagio processo;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        service = new HistoricoProcessoService(
                processoRepository,
                autorizacaoService,
                historicoStatusRepository,
                historicoVinculoRepository,
                pendenciaRepository,
                prazoRepository,
                auditoriaRepository);
        Instituicao instituicao = mock(Instituicao.class);
        lenient().when(instituicao.getId()).thenReturn(7L);
        processo = new ProcessoEstagio();
        processo.setId(1L);
        processo.setInstituicao(instituicao);
        usuario = new Usuario();
        usuario.setPerfil("ALUNO");

        lenient().when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
    }

    @Test
    void deveAgregarFontesSemDuplicarEOrdenarDoMaisRecente() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 8, 10, 0);
        LogAuditoria criacao = auditoria(1L, base, "aluno@teste");
        LogAuditoria resolucao = auditoria(2L, base.plusMinutes(7),
                "instituicao@teste");
        LogAuditoria atualizacaoPrazo = auditoria(3L, base.plusMinutes(10),
                "instituicao@teste");
        when(auditoriaRepository
                .findFirstByInstituicaoIdAndAcaoAndRecursoOrderByDataHoraAscIdAsc(
                        7L, "SOLICITACAO_ESTAGIO", "Processo de estágio 1"))
                .thenReturn(Optional.of(criacao));

        HistoricoStatusProcesso alteracao = historicoStatus(
                10L, base.plusMinutes(5), TipoOperacaoStatus.ALTERACAO_NORMAL, null);
        HistoricoStatusProcesso reabertura = historicoStatus(
                11L, base.plusMinutes(9), TipoOperacaoStatus.REABERTURA,
                "Nova documentação recebida");
        when(historicoStatusRepository.findByProcessoIdOrderByDataHoraAscIdAsc(1L))
                .thenReturn(List.of(alteracao, reabertura));

        List<HistoricoVinculoSupervisor> supervisoes = List.of(
                historicoSupervisor(20L, base.plusMinutes(1),
                        SituacaoVinculoSupervisor.PENDENTE, null),
                historicoSupervisor(21L, base.plusMinutes(2),
                        SituacaoVinculoSupervisor.CONFIRMADO, null),
                historicoSupervisor(22L, base.plusMinutes(3),
                        SituacaoVinculoSupervisor.RECUSADO, "Não sou o responsável"),
                historicoSupervisor(23L, base.plusMinutes(4),
                        SituacaoVinculoSupervisor.CANCELADO, "Substituído"));
        when(historicoVinculoRepository
                .findAllByVinculoProcessoIdOrderByDataHoraAscIdAsc(1L))
                .thenReturn(supervisoes);

        PendenciaProcesso pendencia = mock(PendenciaProcesso.class);
        when(pendencia.getId()).thenReturn(30L);
        when(pendencia.getDescricao()).thenReturn("Entregar termo");
        when(pendencia.getCriadaEm()).thenReturn(base.plusMinutes(6));
        when(pendencia.getResolvidaEm()).thenReturn(base.plusMinutes(7));
        when(pendencia.getResponsavelRegistro()).thenReturn("instituicao@teste");
        when(pendenciaRepository.findAllByProcessoIdOrderByCriadaEmDescIdDesc(1L))
                .thenReturn(List.of(pendencia));
        when(auditoriaRepository
                .findFirstByInstituicaoIdAndAcaoAndRecursoOrderByDataHoraAscIdAsc(
                        7L, "RESOLUCAO_PENDENCIA", "Pendência 30 do processo 1"))
                .thenReturn(Optional.of(resolucao));

        PrazoProcesso prazo = mock(PrazoProcesso.class);
        when(prazo.getId()).thenReturn(40L);
        when(prazo.getDescricao()).thenReturn("Entregar relatório");
        when(prazo.getDataVencimento()).thenReturn(LocalDate.of(2026, 11, 30));
        when(prazo.getCriadaEm()).thenReturn(base.plusMinutes(8));
        when(prazo.getAtualizadaEm()).thenReturn(base.plusMinutes(10));
        when(prazo.getResponsavelRegistro()).thenReturn("instituicao@teste");
        when(prazoRepository.findAllByProcessoIdOrderByDataVencimentoAscIdAsc(1L))
                .thenReturn(List.of(prazo));
        when(auditoriaRepository
                .findFirstByInstituicaoIdAndAcaoAndRecursoOrderByDataHoraDescIdDesc(
                        7L, "ATUALIZACAO_PRAZO", "Prazo 40 do processo 1"))
                .thenReturn(Optional.of(atualizacaoPrazo));

        List<HistoricoProcessoResponse> eventos = service.listar(1L, usuario);

        assertEquals(11, eventos.size());
        assertEquals("ATUALIZACAO_PRAZO", eventos.get(0).tipo());
        assertEquals("CRIACAO_PROCESSO", eventos.get(10).tipo());
        assertEquals(eventos.size(), new HashSet<>(
                eventos.stream().map(HistoricoProcessoResponse::id).toList()).size());
        for (int indice = 1; indice < eventos.size(); indice++) {
            boolean ordemCorreta = !eventos.get(indice - 1).dataHora()
                    .isBefore(eventos.get(indice).dataHora());
            assertTrue(ordemCorreta);
        }
    }

    @Test
    void deveManterResponsavelAusenteQuandoNaoFoiPersistido() {
        PendenciaProcesso pendencia = mock(PendenciaProcesso.class);
        when(pendencia.getId()).thenReturn(30L);
        when(pendencia.getDescricao()).thenReturn("Entregar termo");
        when(pendencia.getCriadaEm()).thenReturn(LocalDateTime.of(2026, 11, 8, 10, 0));
        when(pendencia.getResolvidaEm()).thenReturn(LocalDateTime.of(2026, 11, 8, 11, 0));
        when(pendencia.getResponsavelRegistro()).thenReturn("instituicao@teste");
        when(pendenciaRepository.findAllByProcessoIdOrderByCriadaEmDescIdDesc(1L))
                .thenReturn(List.of(pendencia));

        List<HistoricoProcessoResponse> eventos = service.listar(1L, usuario);

        HistoricoProcessoResponse resolucao = eventos.stream()
                .filter(evento -> "RESOLUCAO_PENDENCIA".equals(evento.tipo()))
                .findFirst()
                .orElseThrow();
        assertNull(resolucao.responsavel());
    }

    @Test
    void deveRetornarVazioQuandoProcessoNaoPossuiMovimentacoesPersistidas() {
        assertEquals(List.of(), service.listar(1L, usuario));
    }

    @Test
    void deveValidarAcessoAntesDeConsultarAsFontesDoHistorico() {
        org.mockito.Mockito.doThrow(new RecursoNaoEncontradoException("Processo não encontrado"))
                .when(autorizacaoService).validarAcesso(processo, usuario);

        assertThrows(RecursoNaoEncontradoException.class, () -> service.listar(1L, usuario));
        verify(historicoStatusRepository, never())
                .findByProcessoIdOrderByDataHoraAscIdAsc(1L);
    }

    @Test
    void deveRetornar404ParaProcessoInexistente() {
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.listar(99L, usuario));
    }

    private HistoricoStatusProcesso historicoStatus(
            Long id,
            LocalDateTime dataHora,
            TipoOperacaoStatus tipo,
            String justificativa) {

        HistoricoStatusProcesso historico = mock(HistoricoStatusProcesso.class);
        when(historico.getId()).thenReturn(id);
        when(historico.getDataHora()).thenReturn(dataHora);
        when(historico.getStatusAnterior()).thenReturn(
                tipo == TipoOperacaoStatus.REABERTURA ? "APROVADO" : "PENDENTE");
        when(historico.getStatusNovo()).thenReturn("EM_ANALISE");
        when(historico.getUsuarioResponsavel()).thenReturn("instituicao@teste");
        when(historico.getJustificativa()).thenReturn(justificativa);
        when(historico.getTipoOperacao()).thenReturn(tipo);
        return historico;
    }

    private HistoricoVinculoSupervisor historicoSupervisor(
            Long id,
            LocalDateTime dataHora,
            SituacaoVinculoSupervisor situacao,
            String justificativa) {

        HistoricoVinculoSupervisor historico = mock(HistoricoVinculoSupervisor.class);
        when(historico.getId()).thenReturn(id);
        when(historico.getDataHora()).thenReturn(dataHora);
        when(historico.getSituacaoNova()).thenReturn(situacao);
        when(historico.getUsuarioResponsavel()).thenReturn("usuario@teste");
        when(historico.getJustificativa()).thenReturn(justificativa);
        return historico;
    }

    private LogAuditoria auditoria(
            Long id,
            LocalDateTime dataHora,
            String responsavel) {

        LogAuditoria log = mock(LogAuditoria.class);
        lenient().when(log.getId()).thenReturn(id);
        lenient().when(log.getDataHora()).thenReturn(dataHora);
        when(log.getUsuario()).thenReturn(responsavel);
        return log;
    }
}
