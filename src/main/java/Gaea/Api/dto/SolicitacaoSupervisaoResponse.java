package Gaea.Api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SolicitacaoSupervisaoResponse(
        Long vinculoId,
        Long processoId,
        String aluno,
        String instituicao,
        String empresa,
        String cnpjEmpresa,
        LocalDate dataInicio,
        LocalDate dataFim,
        LocalDateTime solicitadaEm) {
}
