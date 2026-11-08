package Gaea.Api.service;

import Gaea.Api.dto.HistoricoProcessoResponse;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.HistoricoStatusProcesso;
import Gaea.Api.model.HistoricoVinculoSupervisor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class HistoricoProcessoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ProcessoEstagioRepository processoRepository;
    private final ProcessoAutorizacaoService autorizacaoService;
    private final HistoricoStatusProcessoRepository historicoStatusRepository;
    private final HistoricoVinculoSupervisorRepository historicoVinculoRepository;
    private final PendenciaProcessoRepository pendenciaRepository;
    private final PrazoProcessoRepository prazoRepository;
    private final LogAuditoriaRepository auditoriaRepository;

    public HistoricoProcessoService(
            ProcessoEstagioRepository processoRepository,
            ProcessoAutorizacaoService autorizacaoService,
            HistoricoStatusProcessoRepository historicoStatusRepository,
            HistoricoVinculoSupervisorRepository historicoVinculoRepository,
            PendenciaProcessoRepository pendenciaRepository,
            PrazoProcessoRepository prazoRepository,
            LogAuditoriaRepository auditoriaRepository) {

        this.processoRepository = processoRepository;
        this.autorizacaoService = autorizacaoService;
        this.historicoStatusRepository = historicoStatusRepository;
        this.historicoVinculoRepository = historicoVinculoRepository;
        this.pendenciaRepository = pendenciaRepository;
        this.prazoRepository = prazoRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<HistoricoProcessoResponse> listar(Long processoId, Usuario usuario) {
        ProcessoEstagio processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
        autorizacaoService.validarAcesso(processo, usuario);

        List<HistoricoProcessoResponse> eventos = new ArrayList<>();
        adicionarCriacao(processo, eventos);
        historicoStatusRepository.findByProcessoIdOrderByDataHoraAscIdAsc(processoId)
                .forEach(historico -> eventos.add(eventoStatus(historico)));
        historicoVinculoRepository
                .findAllByVinculoProcessoIdOrderByDataHoraAscIdAsc(processoId)
                .forEach(historico -> eventos.add(eventoSupervisor(historico)));
        pendenciaRepository.findAllByProcessoIdOrderByCriadaEmDescIdDesc(processoId)
                .forEach(pendencia -> adicionarPendencia(processo, pendencia, eventos));
        prazoRepository.findAllByProcessoIdOrderByDataVencimentoAscIdAsc(processoId)
                .forEach(prazo -> adicionarPrazo(processo, prazo, eventos));

        return eventos.stream()
                .filter(evento -> evento.dataHora() != null)
                .sorted(Comparator.comparing(HistoricoProcessoResponse::dataHora)
                        .reversed()
                        .thenComparing(HistoricoProcessoResponse::id))
                .toList();
    }

    private void adicionarCriacao(
            ProcessoEstagio processo,
            List<HistoricoProcessoResponse> eventos) {

        localizarAuditoria(processo, "SOLICITACAO_ESTAGIO",
                "Processo de estágio " + processo.getId())
                .ifPresent(log -> eventos.add(new HistoricoProcessoResponse(
                        "AUDITORIA-" + log.getId(),
                        "CRIACAO_PROCESSO",
                        log.getDataHora(),
                        log.getUsuario(),
                        "Solicitação de estágio criada.")));
    }

    private HistoricoProcessoResponse eventoStatus(HistoricoStatusProcesso historico) {
        boolean reabertura = historico.getTipoOperacao() == TipoOperacaoStatus.REABERTURA;
        String descricao = reabertura
                ? "Processo reaberto de " + historico.getStatusAnterior()
                        + " para " + historico.getStatusNovo() + "."
                : "Status alterado de " + historico.getStatusAnterior()
                        + " para " + historico.getStatusNovo() + ".";
        if (historico.getJustificativa() != null && !historico.getJustificativa().isBlank()) {
            descricao += " Justificativa: " + historico.getJustificativa();
        }

        return new HistoricoProcessoResponse(
                "STATUS-" + historico.getId(),
                reabertura ? "REABERTURA" : "ALTERACAO_STATUS",
                historico.getDataHora(),
                historico.getUsuarioResponsavel(),
                descricao);
    }

    private HistoricoProcessoResponse eventoSupervisor(HistoricoVinculoSupervisor historico) {
        SituacaoVinculoSupervisor situacao = historico.getSituacaoNova();
        String tipo;
        String descricao;
        switch (situacao) {
            case PENDENTE -> {
                tipo = "INDICACAO_SUPERVISOR";
                descricao = "Solicitação de supervisão registrada.";
            }
            case CONFIRMADO -> {
                tipo = "CONFIRMACAO_SUPERVISOR";
                descricao = "Vínculo de supervisão confirmado.";
            }
            case RECUSADO -> {
                tipo = "RECUSA_SUPERVISOR";
                descricao = "Vínculo de supervisão recusado.";
            }
            case CANCELADO -> {
                tipo = "SUBSTITUICAO_SUPERVISOR";
                descricao = "Solicitação anterior de supervisão cancelada para substituição.";
            }
            default -> throw new IllegalStateException("Situação de supervisão não suportada");
        }
        if (historico.getJustificativa() != null && !historico.getJustificativa().isBlank()) {
            descricao += " Justificativa: " + historico.getJustificativa();
        }

        return new HistoricoProcessoResponse(
                "SUPERVISAO-" + historico.getId(),
                tipo,
                historico.getDataHora(),
                historico.getUsuarioResponsavel(),
                descricao);
    }

    private void adicionarPendencia(
            ProcessoEstagio processo,
            PendenciaProcesso pendencia,
            List<HistoricoProcessoResponse> eventos) {

        eventos.add(new HistoricoProcessoResponse(
                "PENDENCIA-CRIACAO-" + pendencia.getId(),
                "CRIACAO_PENDENCIA",
                pendencia.getCriadaEm(),
                pendencia.getResponsavelRegistro(),
                "Pendência registrada: " + pendencia.getDescricao()));

        if (pendencia.getResolvidaEm() != null) {
            String recurso = "Pendência " + pendencia.getId()
                    + " do processo " + processo.getId();
            String responsavel = localizarAuditoria(
                    processo, "RESOLUCAO_PENDENCIA", recurso)
                    .map(LogAuditoria::getUsuario)
                    .orElse(null);
            eventos.add(new HistoricoProcessoResponse(
                    "PENDENCIA-RESOLUCAO-" + pendencia.getId(),
                    "RESOLUCAO_PENDENCIA",
                    pendencia.getResolvidaEm(),
                    responsavel,
                    "Pendência resolvida: " + pendencia.getDescricao()));
        }
    }

    private void adicionarPrazo(
            ProcessoEstagio processo,
            PrazoProcesso prazo,
            List<HistoricoProcessoResponse> eventos) {

        boolean atualizado = prazo.getAtualizadaEm() != null
                && !prazo.getAtualizadaEm().equals(prazo.getCriadaEm());
        String descricaoCriacao = atualizado
                ? "Prazo registrado no processo."
                : descreverPrazo("Prazo registrado", prazo);
        eventos.add(new HistoricoProcessoResponse(
                "PRAZO-CRIACAO-" + prazo.getId(),
                "CRIACAO_PRAZO",
                prazo.getCriadaEm(),
                prazo.getResponsavelRegistro(),
                descricaoCriacao));

        if (atualizado) {
            String recurso = "Prazo " + prazo.getId() + " do processo " + processo.getId();
            String responsavel = localizarAuditoriaMaisRecente(
                    processo, "ATUALIZACAO_PRAZO", recurso)
                    .map(LogAuditoria::getUsuario)
                    .orElse(null);
            eventos.add(new HistoricoProcessoResponse(
                    "PRAZO-ATUALIZACAO-" + prazo.getId(),
                    "ATUALIZACAO_PRAZO",
                    prazo.getAtualizadaEm(),
                    responsavel,
                    descreverPrazo("Prazo atualizado", prazo)));
        }
    }

    private String descreverPrazo(String acao, PrazoProcesso prazo) {
        return acao + ": " + prazo.getDescricao()
                + " (vencimento em " + DATA.format(prazo.getDataVencimento()) + ").";
    }

    private Optional<LogAuditoria> localizarAuditoria(
            ProcessoEstagio processo,
            String acao,
            String recurso) {

        if (processo.getInstituicao() == null || processo.getInstituicao().getId() == null) {
            return Optional.empty();
        }
        return auditoriaRepository
                .findFirstByInstituicaoIdAndAcaoAndRecursoOrderByDataHoraAscIdAsc(
                        processo.getInstituicao().getId(), acao, recurso);
    }

    private Optional<LogAuditoria> localizarAuditoriaMaisRecente(
            ProcessoEstagio processo,
            String acao,
            String recurso) {

        if (processo.getInstituicao() == null || processo.getInstituicao().getId() == null) {
            return Optional.empty();
        }
        return auditoriaRepository
                .findFirstByInstituicaoIdAndAcaoAndRecursoOrderByDataHoraDescIdDesc(
                        processo.getInstituicao().getId(), acao, recurso);
    }
}
