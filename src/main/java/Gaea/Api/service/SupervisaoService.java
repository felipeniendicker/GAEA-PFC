package Gaea.Api.service;

import Gaea.Api.dto.AlteracaoSupervisorRequest;
import Gaea.Api.dto.RecusaSupervisaoRequest;
import Gaea.Api.dto.SolicitacaoSupervisaoResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.HistoricoVinculoSupervisor;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.HistoricoVinculoSupervisorRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupervisaoService {
    private final VinculoSupervisorProcessoRepository vinculoRepository;
    private final ProcessoEstagioRepository processoRepository;
    private final HistoricoVinculoSupervisorRepository historicoRepository;
    private final VerificacaoEmailService verificacaoEmailService;
    private final LogAuditoriaService auditoriaService;

    public SupervisaoService(
            VinculoSupervisorProcessoRepository vinculoRepository,
            ProcessoEstagioRepository processoRepository,
            HistoricoVinculoSupervisorRepository historicoRepository,
            VerificacaoEmailService verificacaoEmailService,
            LogAuditoriaService auditoriaService) {
        this.vinculoRepository = vinculoRepository;
        this.processoRepository = processoRepository;
        this.historicoRepository = historicoRepository;
        this.verificacaoEmailService = verificacaoEmailService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoSupervisaoResponse> listarPendentes(Usuario supervisor) {
        verificacaoEmailService.exigirEmailVerificado(supervisor);
        return vinculoRepository
                .findAllByEmailConvidadoIgnoreCaseAndSituacaoOrderByCriadoEmAsc(
                        supervisor.getEmail(), SituacaoVinculoSupervisor.PENDENTE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void confirmar(Long vinculoId, Usuario supervisor) {
        verificacaoEmailService.exigirEmailVerificado(supervisor);
        VinculoSupervisorProcesso vinculo = buscarPendenteDoSupervisor(vinculoId, supervisor);
        vinculo.setSupervisor(supervisor);
        vinculo.setSituacao(SituacaoVinculoSupervisor.CONFIRMADO);
        vinculo.setRespondidoEm(LocalDateTime.now());
        registrarHistorico(vinculo, SituacaoVinculoSupervisor.PENDENTE,
                SituacaoVinculoSupervisor.CONFIRMADO, supervisor.getEmail(), null);
        auditoriaService.registrar(
                supervisor.getEmail(), "CONFIRMACAO_SUPERVISAO",
                "Vínculo " + vinculo.getId() + " do processo " + vinculo.getProcesso().getId(),
                vinculo.getProcesso().getInstituicao());
    }

    @Transactional
    public void recusar(Long vinculoId, RecusaSupervisaoRequest request, Usuario supervisor) {
        verificacaoEmailService.exigirEmailVerificado(supervisor);
        VinculoSupervisorProcesso vinculo = buscarPendenteDoSupervisor(vinculoId, supervisor);
        vinculo.setSupervisor(supervisor);
        vinculo.setJustificativaRecusa(request.justificativa());
        vinculo.setSituacao(SituacaoVinculoSupervisor.RECUSADO);
        vinculo.setRespondidoEm(LocalDateTime.now());
        registrarHistorico(vinculo, SituacaoVinculoSupervisor.PENDENTE,
                SituacaoVinculoSupervisor.RECUSADO, supervisor.getEmail(), request.justificativa());
        auditoriaService.registrar(
                supervisor.getEmail(), "RECUSA_SUPERVISAO",
                "Vínculo " + vinculo.getId() + " recusado: " + request.justificativa(),
                vinculo.getProcesso().getInstituicao());
    }

    @Transactional
    public void alterarSupervisor(
            Long processoId,
            AlteracaoSupervisorRequest request,
            Usuario aluno) {
        ProcessoEstagio processo = processoRepository.findByIdParaAtualizacao(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
        if (!"ALUNO".equals(aluno.getPerfil())
                || processo.getAluno() == null
                || !processo.getAluno().getId().equals(aluno.getId())) {
            throw new RecursoNaoEncontradoException("Processo não encontrado");
        }
        if (!"PENDENTE".equals(processo.getStatus())) {
            throw new ConflitoDadosException(
                    "O supervisor só pode ser alterado enquanto o processo estiver pendente");
        }

        VinculoSupervisorProcesso anterior = vinculoRepository
                .findFirstByProcessoIdOrderByCriadoEmDescIdDesc(processoId)
                .orElseThrow(() -> new ConflitoDadosException("O processo não possui solicitação de supervisão"));
        if (anterior.getSituacao() != SituacaoVinculoSupervisor.RECUSADO) {
            throw new ConflitoDadosException(
                    "Um novo supervisor só pode ser indicado após a recusa do vínculo atual");
        }
        if (anterior.getEmailConvidado().equalsIgnoreCase(request.emailSupervisor())) {
            throw new ConflitoDadosException("Informe um e-mail diferente para o novo supervisor");
        }

        anterior.setSituacao(SituacaoVinculoSupervisor.CANCELADO);
        registrarHistorico(anterior, SituacaoVinculoSupervisor.RECUSADO,
                SituacaoVinculoSupervisor.CANCELADO, aluno.getEmail(),
                "Substituído por nova indicação do aluno");

        VinculoSupervisorProcesso novo = new VinculoSupervisorProcesso();
        novo.setProcesso(processo);
        novo.setEmailConvidado(request.emailSupervisor());
        novo.setSituacao(SituacaoVinculoSupervisor.PENDENTE);
        vinculoRepository.save(novo);
        registrarHistorico(novo, null, SituacaoVinculoSupervisor.PENDENTE, aluno.getEmail(), null);

        auditoriaService.registrar(
                aluno.getEmail(), "ALTERACAO_SUPERVISOR",
                "Processo " + processoId + ": novo supervisor indicado",
                processo.getInstituicao());
    }

    private VinculoSupervisorProcesso buscarPendenteDoSupervisor(Long id, Usuario supervisor) {
        VinculoSupervisorProcesso vinculo = vinculoRepository.findByIdParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
        if (!vinculo.getEmailConvidado().equalsIgnoreCase(supervisor.getEmail())) {
            throw new RecursoNaoEncontradoException("Solicitação não encontrada");
        }
        if (vinculo.getSituacao() != SituacaoVinculoSupervisor.PENDENTE
                || !Boolean.TRUE.equals(vinculo.getAtivo())) {
            throw new ConflitoDadosException("Esta solicitação já foi respondida");
        }
        return vinculo;
    }

    private void registrarHistorico(
            VinculoSupervisorProcesso vinculo,
            SituacaoVinculoSupervisor anterior,
            SituacaoVinculoSupervisor nova,
            String responsavel,
            String justificativa) {
        HistoricoVinculoSupervisor historico = new HistoricoVinculoSupervisor();
        historico.setVinculo(vinculo);
        historico.setSituacaoAnterior(anterior);
        historico.setSituacaoNova(nova);
        historico.setUsuarioResponsavel(responsavel);
        historico.setJustificativa(justificativa);
        historicoRepository.save(historico);
    }

    private SolicitacaoSupervisaoResponse toResponse(VinculoSupervisorProcesso vinculo) {
        ProcessoEstagio processo = vinculo.getProcesso();
        return new SolicitacaoSupervisaoResponse(
                vinculo.getId(), processo.getId(), processo.getNomeAluno(),
                processo.getInstituicao().getNome(), processo.getNomeEmpresa(),
                processo.getCnpjEmpresa(), processo.getDataInicio(), processo.getDataFim(),
                vinculo.getCriadoEm());
    }
}
