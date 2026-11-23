package Gaea.Api.dto;

import Gaea.Api.model.SolicitacaoPrivacidade;
import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.TipoSolicitacaoPrivacidade;

import java.time.LocalDateTime;

public record SolicitacaoPrivacidadeResponse(
        Long id,
        TipoSolicitacaoPrivacidade tipo,
        String descricao,
        SituacaoSolicitacaoPrivacidade situacao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        String decisao) {

    public static SolicitacaoPrivacidadeResponse from(SolicitacaoPrivacidade solicitacao) {
        return new SolicitacaoPrivacidadeResponse(
                solicitacao.getId(),
                solicitacao.getTipo(),
                solicitacao.getDescricao(),
                solicitacao.getSituacao(),
                solicitacao.getCriadoEm(),
                solicitacao.getAtualizadoEm(),
                solicitacao.getDecisao());
    }
}
