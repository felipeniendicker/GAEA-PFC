package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReaberturaProcessoRequest(
        @NotBlank(message = "A justificativa da reabertura é obrigatória")
        @Size(min = 10, max = 1000, message = "A justificativa deve ter entre 10 e 1000 caracteres")
        String justificativa) {
}
