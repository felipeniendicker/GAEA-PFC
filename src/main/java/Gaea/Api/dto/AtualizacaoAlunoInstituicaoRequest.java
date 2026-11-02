package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizacaoAlunoInstituicaoRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String nome,

        @NotBlank(message = "A matrícula é obrigatória")
        @Size(max = 50, message = "A matrícula deve ter no máximo 50 caracteres")
        String matricula) {

    public AtualizacaoAlunoInstituicaoRequest {
        nome = nome == null ? null : nome.trim();
        matricula = matricula == null ? null : matricula.trim();
    }
}
