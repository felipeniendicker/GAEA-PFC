package Gaea.Api.dto;

import jakarta.validation.constraints.NotNull;

public record AlteracaoSituacaoAlunoRequest(
        @NotNull(message = "Informe a situação do aluno")
        Boolean ativo) {
}
