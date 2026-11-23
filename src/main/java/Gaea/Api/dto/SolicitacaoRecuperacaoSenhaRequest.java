package Gaea.Api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record SolicitacaoRecuperacaoSenhaRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email) {

    public SolicitacaoRecuperacaoSenhaRequest {
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "SolicitacaoRecuperacaoSenhaRequest[email=[PROTEGIDO]]";
    }
}
