package Gaea.Api.service;

import Gaea.Api.dto.CadastroSolicitacaoPrivacidadeRequest;
import Gaea.Api.dto.DecisaoPrivacidadeRequest;
import Gaea.Api.dto.SolicitacaoPrivacidadeAdminResponse;
import Gaea.Api.dto.SolicitacaoPrivacidadeResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.SolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class SolicitacaoPrivacidadeService {

    private final SolicitacaoPrivacidadeRepository repository;
    private final LogAuditoriaService auditoriaService;

    public SolicitacaoPrivacidadeService(
            SolicitacaoPrivacidadeRepository repository,
            LogAuditoriaService auditoriaService) {
        this.repository = repository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public SolicitacaoPrivacidadeResponse cadastrar(
            CadastroSolicitacaoPrivacidadeRequest request,
            Usuario usuario) {
        if (repository.existsByUsuarioIdAndTipoAndSituacaoIn(
                usuario.getId(),
                request.tipo(),
                List.of(SituacaoSolicitacaoPrivacidade.PENDENTE,
                        SituacaoSolicitacaoPrivacidade.EM_ANALISE))) {
            throw new ConflitoDadosException(
                    "Já existe uma solicitação desse tipo em atendimento");
        }
        SolicitacaoPrivacidade solicitacao = new SolicitacaoPrivacidade();
        solicitacao.setUsuario(usuario);
        solicitacao.setTipo(request.tipo());
        solicitacao.setDescricao(request.descricao().trim());
        SolicitacaoPrivacidade salva = repository.saveAndFlush(solicitacao);

        auditoriaService.registrar(
                usuario.getEmail(),
                "SOLICITACAO_PRIVACIDADE",
                "Solicitação " + salva.getId(),
                usuario.getInstituicao());
        return SolicitacaoPrivacidadeResponse.from(salva);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoPrivacidadeResponse> listar(Usuario usuario) {
        return repository.findAllByUsuarioIdOrderByCriadoEmDescIdDesc(usuario.getId())
                .stream()
                .map(SolicitacaoPrivacidadeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoPrivacidadeAdminResponse> listarAdministracao(
            SituacaoSolicitacaoPrivacidade situacao,
            Usuario admin) {
        exigirAdmin(admin);
        List<SolicitacaoPrivacidade> solicitacoes = situacao == null
                ? repository.findAllByOrderByCriadoEmDesc()
                : repository.findAllBySituacaoOrderByCriadoEmAsc(situacao);
        return solicitacoes.stream()
                .map(SolicitacaoPrivacidadeAdminResponse::from)
                .toList();
    }

    @Transactional
    public SolicitacaoPrivacidadeAdminResponse iniciarAnalise(Long id, Usuario admin) {
        exigirAdmin(admin);
        SolicitacaoPrivacidade solicitacao = bloquear(id);
        if (solicitacao.getSituacao() == SituacaoSolicitacaoPrivacidade.EM_ANALISE
                && solicitacao.getResponsavel() != null
                && solicitacao.getResponsavel().getId().equals(admin.getId())) {
            return SolicitacaoPrivacidadeAdminResponse.from(solicitacao);
        }
        if (solicitacao.getSituacao() != SituacaoSolicitacaoPrivacidade.PENDENTE) {
            throw new ConflitoDadosException("A solicitação não está pendente");
        }
        solicitacao.setSituacao(SituacaoSolicitacaoPrivacidade.EM_ANALISE);
        solicitacao.setResponsavel(admin);
        solicitacao.setAtualizadoEm(LocalDateTime.now());
        auditoriaService.registrar(
                admin.getEmail(), "INICIO_ANALISE_PRIVACIDADE",
                "Solicitação de privacidade " + solicitacao.getId());
        return SolicitacaoPrivacidadeAdminResponse.from(solicitacao);
    }

    @Transactional
    public SolicitacaoPrivacidadeAdminResponse concluir(
            Long id,
            DecisaoPrivacidadeRequest request,
            Usuario admin) {
        return decidir(id, request, admin, SituacaoSolicitacaoPrivacidade.CONCLUIDA);
    }

    @Transactional
    public SolicitacaoPrivacidadeAdminResponse rejeitar(
            Long id,
            DecisaoPrivacidadeRequest request,
            Usuario admin) {
        return decidir(id, request, admin, SituacaoSolicitacaoPrivacidade.REJEITADA);
    }

    private SolicitacaoPrivacidadeAdminResponse decidir(
            Long id,
            DecisaoPrivacidadeRequest request,
            Usuario admin,
            SituacaoSolicitacaoPrivacidade destino) {
        exigirAdmin(admin);
        SolicitacaoPrivacidade solicitacao = bloquear(id);
        if (solicitacao.getSituacao() != SituacaoSolicitacaoPrivacidade.EM_ANALISE) {
            throw new ConflitoDadosException("A solicitação não está em análise");
        }
        if (solicitacao.getResponsavel() == null
                || !solicitacao.getResponsavel().getId().equals(admin.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A solicitação está atribuída a outro responsável");
        }
        if (destino == SituacaoSolicitacaoPrivacidade.CONCLUIDA
                && request.evidenciaNormalizada() == null) {
            throw new ConflitoDadosException(
                    "Registre a evidência da execução antes de concluir");
        }
        solicitacao.setSituacao(destino);
        solicitacao.setDecisao(request.decisaoNormalizada());
        solicitacao.setEvidenciaExecucao(request.evidenciaNormalizada());
        solicitacao.setAtualizadoEm(LocalDateTime.now());
        auditoriaService.registrar(
                admin.getEmail(),
                destino == SituacaoSolicitacaoPrivacidade.CONCLUIDA
                        ? "CONCLUSAO_PRIVACIDADE" : "REJEICAO_PRIVACIDADE",
                "Solicitação de privacidade " + solicitacao.getId());
        return SolicitacaoPrivacidadeAdminResponse.from(solicitacao);
    }

    private SolicitacaoPrivacidade bloquear(Long id) {
        return repository.findByIdParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Solicitação de privacidade não encontrada"));
    }

    private void exigirAdmin(Usuario usuario) {
        if (usuario == null || !"ADMIN_GAEA".equals(usuario.getPerfil())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Acesso administrativo necessário");
        }
    }
}
