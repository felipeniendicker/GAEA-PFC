package Gaea.Api.dto;

import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.SolicitacaoPrivacidade;
import Gaea.Api.model.TipoSolicitacaoPrivacidade;

import java.time.LocalDateTime;

public record SolicitacaoPrivacidadeAdminResponse(
        Long id,
        TipoSolicitacaoPrivacidade tipo,
        String descricao,
        SituacaoSolicitacaoPrivacidade situacao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        Long titularId,
        String titularNome,
        String titularEmail,
        String titularPerfil,
        String instituicao,
        String responsavel,
        String decisao,
        String evidenciaExecucao) {

    public static SolicitacaoPrivacidadeAdminResponse from(SolicitacaoPrivacidade solicitacao) {
        var titular = solicitacao.getUsuario();
        var responsavel = solicitacao.getResponsavel();
        return new SolicitacaoPrivacidadeAdminResponse(
                solicitacao.getId(),
                solicitacao.getTipo(),
                solicitacao.getDescricao(),
                solicitacao.getSituacao(),
                solicitacao.getCriadoEm(),
                solicitacao.getAtualizadoEm(),
                titular.getId(),
                titular.getNome(),
                titular.getEmail(),
                titular.getPerfil(),
                titular.getInstituicao() == null ? null : titular.getInstituicao().getNome(),
                responsavel == null ? null : responsavel.getEmail(),
                solicitacao.getDecisao(),
                solicitacao.getEvidenciaExecucao());
    }
}
