package Gaea.Api.service;

import Gaea.Api.dto.CadastroSolicitacaoPrivacidadeRequest;
import Gaea.Api.dto.DecisaoPrivacidadeRequest;
import Gaea.Api.dto.EventoPrivacidadeResponse;
import Gaea.Api.dto.SolicitacaoPrivacidadeAdminResponse;
import Gaea.Api.dto.SolicitacaoPrivacidadeResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.SolicitacaoPrivacidade;
import Gaea.Api.model.TipoSolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

@Service
public class SolicitacaoPrivacidadeService {

    private final SolicitacaoPrivacidadeRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaPrivacidadeService auditoria;
    private final ExecucaoSolicitacaoPrivacidadeService executor;
    private final ComunicacaoPrivacidadeService comunicacao;

    public SolicitacaoPrivacidadeService(
            SolicitacaoPrivacidadeRepository repository,
            PasswordEncoder passwordEncoder,
            AuditoriaPrivacidadeService auditoria,
            ExecucaoSolicitacaoPrivacidadeService executor,
            ComunicacaoPrivacidadeService comunicacao) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
        this.executor = executor;
        this.comunicacao = comunicacao;
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
        validarIdentidadeParaEncerramento(request, usuario);
        SolicitacaoPrivacidade solicitacao = new SolicitacaoPrivacidade();
        solicitacao.setUsuario(usuario);
        solicitacao.setTipo(request.tipo());
        solicitacao.setDescricao(request.descricao().trim());
        SolicitacaoPrivacidade salva = repository.saveAndFlush(solicitacao);

        auditoria.registrar(
                usuario.getEmail(), "SOLICITACAO_PRIVACIDADE", salva.getId());
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
        auditoria.registrar(
                admin.getEmail(), "INICIO_ANALISE_PRIVACIDADE", solicitacao.getId());
        return SolicitacaoPrivacidadeAdminResponse.from(solicitacao);
    }

    public SolicitacaoPrivacidadeAdminResponse concluir(
            Long id,
            DecisaoPrivacidadeRequest request,
            Usuario admin) {
        try {
            return executor.executarEncerramento(id, request, admin);
        } catch (RuntimeException exception) {
            auditoria.registrarFalha(
                    admin == null ? "DESCONHECIDO" : admin.getEmail(), id);
            throw exception;
        }
    }

    @Transactional
    public SolicitacaoPrivacidadeAdminResponse rejeitar(
            Long id,
            DecisaoPrivacidadeRequest request,
            Usuario admin) {
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
        solicitacao.setSituacao(SituacaoSolicitacaoPrivacidade.REJEITADA);
        solicitacao.setDecisao(request.decisaoNormalizada());
        solicitacao.setEvidenciaExecucao(null);
        solicitacao.setAtualizadoEm(LocalDateTime.now());
        auditoria.registrar(admin.getEmail(), "REJEICAO_PRIVACIDADE", solicitacao.getId());
        comunicacao.agendarAposCommit(
                solicitacao.getId(),
                solicitacao.getUsuario().getEmail(),
                solicitacao.getUsuario().getNome(),
                false);
        return SolicitacaoPrivacidadeAdminResponse.from(solicitacao);
    }

    @Transactional(readOnly = true)
    public List<EventoPrivacidadeResponse> listarAuditoria(Long id, Usuario admin) {
        exigirAdmin(admin);
        if (!repository.existsById(id)) {
            throw new RecursoNaoEncontradoException(
                    "Solicitação de privacidade não encontrada");
        }
        return auditoria.listar(id);
    }

    private void validarIdentidadeParaEncerramento(
            CadastroSolicitacaoPrivacidadeRequest request,
            Usuario usuario) {
        if (request.tipo() != TipoSolicitacaoPrivacidade.ENCERRAMENTO_CONTA) {
            return;
        }
        if (request.senhaAtual() == null
                || usuario.getSenha() == null
                || !passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new DadosInvalidosException("Confirmação de identidade inválida");
        }
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
