package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtividadePlanoRequest(
        @NotBlank(message = "A descrição da atividade é obrigatória")
        @Size(max = 1000, message = "A atividade deve ter no máximo 1000 caracteres")
        String descricao) {

    public String descricaoNormalizada() {
        return descricao.trim();
    }
}
