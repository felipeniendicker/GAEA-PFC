package Gaea.Api.dto;

import Gaea.Api.model.SituacaoCargaHoraria;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoCargaHorariaRequest(
        @NotNull(message = "Informe o resultado da avaliação")
        SituacaoCargaHoraria resultado,

        @Size(max = 1000, message = "A justificativa deve ter no máximo 1000 caracteres")
        String justificativa) {

    @AssertTrue(message = "A justificativa é obrigatória para rejeitar o registro")
    public boolean isJustificativaValida() {
        return resultado != SituacaoCargaHoraria.REJEITADO
                || justificativa != null && !justificativa.isBlank();
    }

    public String justificativaNormalizada() {
        return justificativa == null || justificativa.isBlank() ? null : justificativa.trim();
    }
}
