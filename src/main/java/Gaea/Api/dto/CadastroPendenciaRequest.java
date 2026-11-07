package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroPendenciaRequest(
        @NotBlank(message = "A descrição da pendência é obrigatória")
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
        String descricao) {

    public CadastroPendenciaRequest {
        descricao = descricao == null ? null : descricao.trim();
    }
}
