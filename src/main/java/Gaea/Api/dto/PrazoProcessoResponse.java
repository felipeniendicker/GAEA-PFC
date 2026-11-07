package Gaea.Api.dto;

import Gaea.Api.model.PrazoProcesso;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PrazoProcessoResponse(
        Long id,
        Long processoId,
        String descricao,
        LocalDate dataVencimento,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm,
        String responsavelRegistro,
        Long versao) {

    public static PrazoProcessoResponse from(PrazoProcesso prazo) {
        return new PrazoProcessoResponse(
                prazo.getId(),
                prazo.getProcesso().getId(),
                prazo.getDescricao(),
                prazo.getDataVencimento(),
                prazo.getCriadaEm(),
                prazo.getAtualizadaEm(),
                prazo.getResponsavelRegistro(),
                prazo.getVersao());
    }
}
