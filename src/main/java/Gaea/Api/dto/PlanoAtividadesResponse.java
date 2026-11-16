package Gaea.Api.dto;

import Gaea.Api.model.PlanoAtividades;
import Gaea.Api.model.SituacaoPlanoAtividades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PlanoAtividadesResponse(
        Long id,
        Long processoId,
        List<AtividadePlanoResponse> atividades,
        BigDecimal cargaHorariaPlanejada,
        SituacaoPlanoAtividades situacao,
        LocalDateTime criadoEm,
        String criadoPor,
        LocalDateTime avaliadoEm,
        String avaliadoPor,
        String justificativaRejeicao) {

    public static PlanoAtividadesResponse from(PlanoAtividades plano) {
        return new PlanoAtividadesResponse(
                plano.getId(),
                plano.getProcesso().getId(),
                plano.getAtividades().stream().map(AtividadePlanoResponse::from).toList(),
                plano.getCargaHorariaPlanejada(),
                plano.getSituacao(),
                plano.getCriadoEm(),
                plano.getCriadoPor().getNome(),
                plano.getAvaliadoEm(),
                plano.getAvaliadoPor() == null ? null : plano.getAvaliadoPor().getNome(),
                plano.getJustificativaRejeicao());
    }
}
