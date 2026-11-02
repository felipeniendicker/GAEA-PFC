package Gaea.Api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtivacaoAlunoRequest(
        @NotBlank(message = "O token de ativação é obrigatório")
        @Size(min = 40, max = 200, message = "O convite de ativação é inválido")
        String token,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha,

        @NotNull(message = "Informe o aceite dos Termos de Uso e da Política de Privacidade")
        @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade")
        Boolean termosAceitos) {

    @Override
    public String toString() {
        return "AtivacaoAlunoRequest[token=[PROTEGIDO], senha=[PROTEGIDA], termosAceitos="
                + termosAceitos + "]";
    }
}
