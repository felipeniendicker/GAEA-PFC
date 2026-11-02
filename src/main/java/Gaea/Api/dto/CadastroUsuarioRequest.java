package Gaea.Api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CadastroUsuarioRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres")
        String senha,

        @NotBlank(message = "O perfil é obrigatório")
        @Pattern(regexp = "ALUNO|EMPRESA", message = "O perfil deve ser ALUNO ou EMPRESA")
        String perfil,

        @NotNull(message = "Informe o aceite dos Termos de Uso e da Política de Privacidade")
        @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade")
        Boolean termosAceitos) {

    public CadastroUsuarioRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
