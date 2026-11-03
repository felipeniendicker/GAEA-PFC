package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecusaSupervisaoRequest(
        @NotBlank(message = "A justificativa da recusa é obrigatória")
        @Size(min = 10, max = 1000, message = "A justificativa deve ter entre 10 e 1000 caracteres")
        String justificativa) {

    public RecusaSupervisaoRequest {
        justificativa = justificativa == null ? null : justificativa.trim();
    }
}
