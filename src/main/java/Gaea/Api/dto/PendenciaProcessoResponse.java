package Gaea.Api.dto;

import Gaea.Api.model.PendenciaProcesso;

import java.time.LocalDateTime;

public record PendenciaProcessoResponse(
        Long id,
        Long processoId,
        String descricao,
        String situacao,
        LocalDateTime criadaEm,
        LocalDateTime resolvidaEm,
        String responsavelRegistro) {

    public static PendenciaProcessoResponse from(PendenciaProcesso pendencia) {
        return new PendenciaProcessoResponse(
                pendencia.getId(),
                pendencia.getProcesso().getId(),
                pendencia.getDescricao(),
                pendencia.getSituacao().name(),
                pendencia.getCriadaEm(),
                pendencia.getResolvidaEm(),
                pendencia.getResponsavelRegistro());
    }
}
