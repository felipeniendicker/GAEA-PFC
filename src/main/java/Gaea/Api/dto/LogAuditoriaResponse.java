package Gaea.Api.dto;

import Gaea.Api.model.LogAuditoria;

import java.time.LocalDateTime;

public record LogAuditoriaResponse(
        Long id,
        String usuario,
        String acao,
        String recurso,
        LocalDateTime dataHora) {

    public static LogAuditoriaResponse from(LogAuditoria log) {
        return new LogAuditoriaResponse(
                log.getId(),
                log.getUsuario(),
                log.getAcao(),
                log.getRecurso(),
                log.getDataHora());
    }
}
