package Gaea.Api.dto;

import java.time.LocalDate;

public record RelatorioProcessoResponse(
        Long processoId,
        String aluno,
        String empresa,
        String supervisor,
        String situacao,
        LocalDate dataInicio,
        LocalDate dataFim) {
}
