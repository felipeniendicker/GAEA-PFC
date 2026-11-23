package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DecisaoPrivacidadeRequest(
        @NotBlank(message = "Informe a decisão ou justificativa")
        @Size(min = 10, max = 2000, message = "A decisão deve ter entre 10 e 2000 caracteres")
        String decisao,

        @Size(max = 2000, message = "A evidência deve ter no máximo 2000 caracteres")
        String evidenciaExecucao) {

    public String decisaoNormalizada() {
        return decisao.trim();
    }

    public String evidenciaNormalizada() {
        return evidenciaExecucao == null || evidenciaExecucao.isBlank()
                ? null : evidenciaExecucao.trim();
    }
}
