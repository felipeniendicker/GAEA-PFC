package Gaea.Api.dto;

import Gaea.Api.model.SituacaoDocumento;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnaliseDocumentoRequest(
        @NotNull(message = "Informe o resultado da análise")
        SituacaoDocumento resultado,

        @Size(max = 1000, message = "A justificativa deve ter no máximo 1000 caracteres")
        String justificativa) {

    @AssertTrue(message = "A justificativa é obrigatória para reprovar o documento")
    public boolean isJustificativaValida() {
        if (resultado != SituacaoDocumento.REPROVADO) {
            return true;
        }
        return justificativa != null && !justificativa.isBlank();
    }

    public String justificativaNormalizada() {
        return justificativa == null || justificativa.isBlank() ? null : justificativa.trim();
    }
}
