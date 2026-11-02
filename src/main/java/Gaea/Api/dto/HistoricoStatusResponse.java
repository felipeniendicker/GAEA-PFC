package Gaea.Api.dto;

import Gaea.Api.model.HistoricoStatusProcesso;

import java.time.LocalDateTime;

public record HistoricoStatusResponse(
        Long id,
        Long processoId,
        String statusAnterior,
        String statusNovo,
        String usuarioResponsavel,
        LocalDateTime dataHora,
        String justificativa,
        String tipoOperacao) {

    public static HistoricoStatusResponse from(HistoricoStatusProcesso historico) {
        return new HistoricoStatusResponse(
                historico.getId(),
                historico.getProcesso().getId(),
                historico.getStatusAnterior(),
                historico.getStatusNovo(),
                historico.getUsuarioResponsavel(),
                historico.getDataHora(),
                historico.getJustificativa(),
                historico.getTipoOperacao().name());
    }
}
