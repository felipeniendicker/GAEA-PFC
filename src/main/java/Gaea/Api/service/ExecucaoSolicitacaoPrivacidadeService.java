package Gaea.Api.service;

import Gaea.Api.dto.DecisaoPrivacidadeRequest;
import Gaea.Api.dto.SolicitacaoPrivacidadeAdminResponse;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.RecursoNaoEncontradoException;
import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.SolicitacaoPrivacidade;
import Gaea.Api.model.TipoSolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.ConviteAtivacaoAlunoRepository;
import Gaea.Api.repository.SolicitacaoPrivacidadeRepository;
import Gaea.Api.repository.SolicitacaoRecuperacaoSenhaRepository;
import Gaea.Api.repository.TokenFluxoInstituicaoRepository;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ExecucaoSolicitacaoPrivacidadeService {

    private final SolicitacaoPrivacidadeRepository solicitacoes;
    private final UsuarioRepository usuarios;
    private final SolicitacaoRecuperacaoSenhaRepository recuperacoes;
    private final ConviteAtivacaoAlunoRepository convitesAluno;
    private final TokenVerificacaoEmailRepository verificacoesEmail;
    private final TokenFluxoInstituicaoRepository tokensInstituicao;
    private final AuditoriaPrivacidadeService auditoria;
    private final ComunicacaoPrivacidadeService comunicacao;

    public ExecucaoSolicitacaoPrivacidadeService(
            SolicitacaoPrivacidadeRepository solicitacoes,
            UsuarioRepository usuarios,
            SolicitacaoRecuperacaoSenhaRepository recuperacoes,
            ConviteAtivacaoAlunoRepository convitesAluno,
            TokenVerificacaoEmailRepository verificacoesEmail,
            TokenFluxoInstituicaoRepository tokensInstituicao,
            AuditoriaPrivacidadeService auditoria,
            ComunicacaoPrivacidadeService comunicacao) {
        this.solicitacoes = solicitacoes;
        this.usuarios = usuarios;
        this.recuperacoes = recuperacoes;
        this.convitesAluno = convitesAluno;
        this.verificacoesEmail = verificacoesEmail;
        this.tokensInstituicao = tokensInstituicao;
        this.auditoria = auditoria;
        this.comunicacao = comunicacao;
    }

    @Transactional
    public SolicitacaoPrivacidadeAdminResponse executarEncerramento(
            Long id,
            DecisaoPrivacidadeRequest request,
            Usuario admin) {
        exigirAdmin(admin);
        SolicitacaoPrivacidade solicitacao = bloquearSolicitacao(id);
        if (encerramentoJaExecutado(solicitacao)) {
            return SolicitacaoPrivacidadeAdminResponse.from(solicitacao);
        }
        validarResponsavelEEstado(solicitacao, admin);
        validarOperacaoExecutavel(solicitacao);

        Usuario titular = bloquearTitular(solicitacao);

        LocalDateTime agora = LocalDateTime.now();
        auditoria.registrar(admin.getEmail(), "APROVACAO_PRIVACIDADE", id);
        titular.setAtivo(false);
        titular.setCredenciaisVersao(versaoCredenciais(titular) + 1);
        int tokensCancelados = cancelarTokensPendentes(titular.getId(), agora);

        solicitacao.setSituacao(SituacaoSolicitacaoPrivacidade.CONCLUIDA);
        solicitacao.setDecisao(request.decisaoNormalizada());
        solicitacao.setEvidenciaExecucao(
                "Conta desativada, credenciais revogadas e " + tokensCancelados
                        + " token(s) pendente(s) cancelado(s) em " + agora + ".");
        solicitacao.setAtualizadoEm(agora);

        auditoria.registrar(admin.getEmail(), "EXECUCAO_PRIVACIDADE", id);
        auditoria.registrar(admin.getEmail(), "DESATIVACAO_CONTA_PRIVACIDADE", id);
        comunicacao.agendarAposCommit(
                id, titular.getEmail(), titular.getNome(), true);
        return SolicitacaoPrivacidadeAdminResponse.from(solicitacao);
    }

    private void validarOperacaoExecutavel(SolicitacaoPrivacidade solicitacao) {
        if (solicitacao.getTipo() == TipoSolicitacaoPrivacidade.EXCLUSAO
                || solicitacao.getTipo() == TipoSolicitacaoPrivacidade.ANONIMIZACAO) {
            throw new ConflitoDadosException(
                    "A solicitação depende da definição institucional dos dados elegíveis e deve permanecer em análise");
        }
        if (solicitacao.getTipo() != TipoSolicitacaoPrivacidade.ENCERRAMENTO_CONTA) {
            throw new ConflitoDadosException(
                    "Este tipo de solicitação ainda não possui execução técnica automatizada e deve permanecer em análise");
        }
    }

    private void validarResponsavelEEstado(
            SolicitacaoPrivacidade solicitacao,
            Usuario admin) {
        if (solicitacao.getSituacao() != SituacaoSolicitacaoPrivacidade.EM_ANALISE) {
            throw new ConflitoDadosException("A solicitação não está em análise");
        }
        if (solicitacao.getResponsavel() == null
                || !solicitacao.getResponsavel().getId().equals(admin.getId())) {
            throw new AccessDeniedException(
                    "A solicitação está atribuída a outro responsável");
        }
    }

    private Usuario bloquearTitular(SolicitacaoPrivacidade solicitacao) {
        Usuario referencia = solicitacao.getUsuario();
        if ("INSTITUICAO".equals(referencia.getPerfil())
                && referencia.getInstituicao() != null
                && Boolean.TRUE.equals(referencia.getAtivo())) {
            var responsaveis = usuarios
                    .findResponsaveisInstitucionaisAtivosParaAtualizacao(
                            referencia.getInstituicao().getId());
            if (responsaveis.size() <= 1) {
                throw new ConflitoDadosException(
                        "Cadastre e ative outro responsável institucional antes de encerrar esta conta");
            }
            return responsaveis.stream()
                    .filter(usuario -> usuario.getId().equals(referencia.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Titular não encontrado"));
        }
        return usuarios.findByIdParaAtualizacao(referencia.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Titular não encontrado"));
    }

    private int cancelarTokensPendentes(Long usuarioId, LocalDateTime agora) {
        var tokensRecuperacao = recuperacoes
                .findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(usuarioId);
        tokensRecuperacao.forEach(token -> token.setCanceladoEm(agora));

        var tokensConvite = convitesAluno
                .findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(usuarioId);
        tokensConvite.forEach(token -> token.setCanceladoEm(agora));

        var tokensVerificacao = verificacoesEmail
                .findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(usuarioId);
        tokensVerificacao.forEach(token -> token.setCanceladoEm(agora));

        var tokensFluxoInstituicao = tokensInstituicao
                .findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(usuarioId);
        tokensFluxoInstituicao.forEach(token -> token.setCanceladoEm(agora));

        return tokensRecuperacao.size()
                + tokensConvite.size()
                + tokensVerificacao.size()
                + tokensFluxoInstituicao.size();
    }

    private boolean encerramentoJaExecutado(SolicitacaoPrivacidade solicitacao) {
        return solicitacao.getTipo() == TipoSolicitacaoPrivacidade.ENCERRAMENTO_CONTA
                && solicitacao.getSituacao() == SituacaoSolicitacaoPrivacidade.CONCLUIDA
                && !Boolean.TRUE.equals(solicitacao.getUsuario().getAtivo());
    }

    private SolicitacaoPrivacidade bloquearSolicitacao(Long id) {
        return solicitacoes.findByIdParaAtualizacao(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Solicitação de privacidade não encontrada"));
    }

    private long versaoCredenciais(Usuario usuario) {
        return usuario.getCredenciaisVersao() == null ? 0L : usuario.getCredenciaisVersao();
    }

    private void exigirAdmin(Usuario usuario) {
        if (usuario == null || !"ADMIN_GAEA".equals(usuario.getPerfil())) {
            throw new AccessDeniedException("Acesso administrativo necessário");
        }
    }
}
