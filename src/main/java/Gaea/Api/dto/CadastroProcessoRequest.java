package Gaea.Api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

import java.time.LocalDate;
import java.util.Locale;

public record CadastroProcessoRequest(
        @NotBlank(message = "O nome da empresa é obrigatório")
        @Size(max = 255, message = "O nome da empresa deve ter no máximo 255 caracteres")
        String nomeEmpresa,

        @NotBlank(message = "O CNPJ é obrigatório")
        @CNPJ(message = "Informe um CNPJ válido")
        String cnpjEmpresa,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "(?:\\d{5}-?\\d{3})", message = "Informe um CEP válido")
        String cepEmpresa,

        @NotBlank(message = "O endereço da empresa é obrigatório")
        @Size(max = 500, message = "O endereço deve ter no máximo 500 caracteres")
        String enderecoEmpresa,

        @NotNull(message = "A data inicial é obrigatória")
        LocalDate dataInicio,

        @NotNull(message = "A data final é obrigatória")
        LocalDate dataFim,

        @NotBlank(message = "O e-mail do supervisor é obrigatório")
        @Email(message = "Informe um e-mail de supervisor válido")
        @Size(max = 255, message = "O e-mail do supervisor deve ter no máximo 255 caracteres")
        String emailSupervisor) {

    public CadastroProcessoRequest {
        nomeEmpresa = limpar(nomeEmpresa);
        cnpjEmpresa = cnpjEmpresa == null ? null : cnpjEmpresa.replaceAll("\\D", "");
        cepEmpresa = cepEmpresa == null ? null : cepEmpresa.replaceAll("\\D", "");
        enderecoEmpresa = limpar(enderecoEmpresa);
        emailSupervisor = emailSupervisor == null
                ? null
                : emailSupervisor.trim().toLowerCase(Locale.ROOT);
    }

    @JsonIgnore
    @AssertTrue(message = "A data final não pode ser anterior à data inicial")
    public boolean isPeriodoValido() {
        return dataInicio == null || dataFim == null || !dataFim.isBefore(dataInicio);
    }

    private static String limpar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
