package Gaea.Api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.br.CNPJ;

import java.time.LocalDate;

public record CadastroProcessoRequest(
        @NotBlank(message = "O nome do aluno é obrigatório")
        String nomeAluno,

        @NotBlank(message = "O e-mail do aluno é obrigatório")
        @Email(message = "Informe um e-mail de aluno válido")
        String emailAluno,

        @NotBlank(message = "O nome da empresa é obrigatório")
        String nomeEmpresa,

        @NotBlank(message = "O CNPJ é obrigatório")
        @CNPJ(message = "Informe um CNPJ válido")
        String cnpjEmpresa,

        @NotNull(message = "A data inicial é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "A data final é obrigatória")
        LocalDate dataFim) {

    @JsonIgnore
    @AssertTrue(message = "A data final não pode ser anterior à data inicial")
    public boolean isPeriodoValido() {
        return dataInicio == null || dataFim == null || !dataFim.isBefore(dataInicio);
    }
}
