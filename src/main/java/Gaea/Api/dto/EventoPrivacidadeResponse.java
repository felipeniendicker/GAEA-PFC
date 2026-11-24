package Gaea.Api.dto;

import Gaea.Api.model.LogAuditoria;

import java.time.LocalDateTime;

public record EventoPrivacidadeResponse(
        LocalDateTime dataHora,
        String acao,
        String ator) {

    public static EventoPrivacidadeResponse from(LogAuditoria log) {
        return new EventoPrivacidadeResponse(
                log.getDataHora(),
                log.getAcao(),
                log.getUsuario());
    }
}
