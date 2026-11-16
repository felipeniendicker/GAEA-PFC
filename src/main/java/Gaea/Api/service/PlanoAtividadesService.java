package Gaea.Api.service;

import Gaea.Api.dto.AvaliacaoPlanoAtividadesRequest;
import Gaea.Api.dto.CadastroPlanoAtividadesRequest;
import Gaea.Api.dto.PlanoAtividadesResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.PlanoAtividades;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoPlanoAtividades;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.PlanoAtividadesRepository;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PlanoAtividadesService {

    private final PlanoAtividadesRepository planoRepository;
    private final ProcessoEstagioRepository processoRepository;
    private final VinculoSupervisorProcessoRepository vinculoRepository;
    private final ProcessoAutorizacaoService autorizacaoService;
    private final LogAuditoriaService auditoriaService;

    public PlanoAtividadesService(
            PlanoAtividadesRepository planoRepository,
            ProcessoEstagioRepository processoRepository,
            VinculoSupervisorProcessoRepository vinculoRepository,
            ProcessoAutorizacaoService autorizacaoService,
            LogAuditoriaService auditoriaService) {
        this.planoRepository = planoRepository;
        this.processoRepository = processoRepository;
        this.vinculoRepository = vinculoRepository;
        this.autorizacaoService = autorizacaoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public PlanoAtividadesResponse cadastrar(
            Long processoId,
            CadastroPlanoAtividadesRequest request,
            Usuario aluno) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        validarAlunoProprietario(processo, aluno);
        if (planoRepository.existsByProcessoId(processoId)) {
            throw new ConflitoDadosException("O processo já possui um plano de atividades");
        }
        PlanoAtividades plano = new PlanoAtividades();
        plano.setProcesso(processo);
        plano.setCriadoPor(aluno);
        plano.setCargaHorariaPlanejada(request.cargaHorariaPlanejada());
        plano.setSituacao(SituacaoPlanoAtividades.PENDENTE);
        for (int indice = 0; indice < request.atividades().size(); indice++) {
            plano.adicionarAtividade(
                    request.atividades().get(indice).descricaoNormalizada(), indice + 1);
        }
        PlanoAtividades salvo = planoRepository.saveAndFlush(plano);

        auditoriaService.registrar(
                aluno.getEmail(),
                "CADASTRO_PLANO_ATIVIDADES",
                "Plano " + salvo.getId() + " do processo " + processoId,
                processo.getInstituicao());
        return PlanoAtividadesResponse.from(salvo);
    }

    @Transactional(readOnly = true)
    public PlanoAtividadesResponse consultar(Long processoId, Usuario usuario) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        autorizacaoService.validarAcesso(processo, usuario);
        PlanoAtividades plano = planoRepository.findByProcessoId(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Plano de atividades não encontrado"));
        return PlanoAtividadesResponse.from(plano);
    }

    @Transactional
    public PlanoAtividadesResponse avaliar(
            Long processoId,
            AvaliacaoPlanoAtividadesRequest request,
            Usuario supervisor) {
        ProcessoEstagio processo = buscarProcesso(processoId);
        validarSupervisorConfirmado(processo, supervisor);
        if (request.resultado() == SituacaoPlanoAtividades.PENDENTE) {
            throw new DadosInvalidosException("O resultado deve ser APROVADO ou REJEITADO");
        }

        PlanoAtividades plano = planoRepository.findByProcessoIdParaAtualizacao(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Plano de atividades não encontrado"));
        if (plano.getSituacao() != SituacaoPlanoAtividades.PENDENTE) {
            throw new ConflitoDadosException("O plano de atividades já foi avaliado");
        }

        plano.setSituacao(request.resultado());
        plano.setAvaliadoPor(supervisor);
        plano.setAvaliadoEm(LocalDateTime.now());
        plano.setJustificativaRejeicao(
                request.resultado() == SituacaoPlanoAtividades.REJEITADO
                        ? request.justificativaNormalizada() : null);
        PlanoAtividades salvo = planoRepository.save(plano);

        auditoriaService.registrar(
                supervisor.getEmail(),
                request.resultado() == SituacaoPlanoAtividades.APROVADO
                        ? "APROVACAO_PLANO_ATIVIDADES" : "REJEICAO_PLANO_ATIVIDADES",
                "Plano " + salvo.getId() + " do processo " + processoId,
                processo.getInstituicao());
        return PlanoAtividadesResponse.from(salvo);
    }

    private ProcessoEstagio buscarProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado"));
    }

    private void validarAlunoProprietario(ProcessoEstagio processo, Usuario usuario) {
        if (usuario == null || !"ALUNO".equals(usuario.getPerfil())) {
            throw new AccessDeniedException("Somente o aluno pode cadastrar o plano de atividades");
        }
        autorizacaoService.validarAcesso(processo, usuario);
    }

    private void validarSupervisorConfirmado(ProcessoEstagio processo, Usuario usuario) {
        if (usuario == null || !"EMPRESA".equals(usuario.getPerfil())) {
            throw new AccessDeniedException("Somente o supervisor pode avaliar o plano de atividades");
        }
        boolean confirmado = vinculoRepository
                .existsByProcessoIdAndSupervisorIdAndSituacaoAndAtivoTrue(
                        processo.getId(), usuario.getId(), SituacaoVinculoSupervisor.CONFIRMADO);
        if (!confirmado) {
            throw new RecursoNaoEncontradoException("Processo não encontrado");
        }
    }

}
