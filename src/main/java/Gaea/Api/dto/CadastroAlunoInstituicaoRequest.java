package Gaea.Api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CadastroAlunoInstituicaoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "A matrícula é obrigatória")
        @Size(max = 50, message = "A matrícula deve ter no máximo 50 caracteres")
        String matricula) {

    public CadastroAlunoInstituicaoRequest {
        nome = nome == null ? null : nome.trim();
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        matricula = matricula == null ? null : matricula.trim();
    }
}
