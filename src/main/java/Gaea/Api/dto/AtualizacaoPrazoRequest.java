package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizacaoPrazoRequest(
        @NotBlank(message = "A descrição do prazo é obrigatória")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao,

        @NotNull(message = "A data de vencimento é obrigatória")
        LocalDate dataVencimento) {

    public AtualizacaoPrazoRequest {
        descricao = descricao == null ? null : descricao.trim();
    }
}
