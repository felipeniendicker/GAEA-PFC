package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmacaoEmailRequest(
        @NotBlank(message = "O token de verificação é obrigatório")
        String token) {

    @Override
    public String toString() {
        return "ConfirmacaoEmailRequest[token=[PROTEGIDO]]";
    }
}
