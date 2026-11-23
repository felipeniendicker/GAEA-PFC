package Gaea.Api.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CNPJ;
import java.util.Locale;

public record SolicitacaoInstituicaoRequest(
        @NotBlank @Size(max=255) String nomeInstitucional,
        @NotBlank @CNPJ(message="Informe um CNPJ válido") String cnpj,
        @NotBlank @Size(max=255) String responsavelNome,
        @NotBlank @Email(message="Informe um e-mail institucional válido") @Size(max=255) String emailInstitucional,
        @NotNull @AssertTrue(message="É necessário aceitar os Termos de Uso e a Política de Privacidade")
        Boolean termosAceitos) {
    public SolicitacaoInstituicaoRequest {
        nomeInstitucional = limpar(nomeInstitucional);
        cnpj = cnpj == null ? null : cnpj.replaceAll("\\D", "");
        responsavelNome = limpar(responsavelNome);
        emailInstitucional = emailInstitucional == null ? null : emailInstitucional.trim().toLowerCase(Locale.ROOT);
    }
    private static String limpar(String v) { return v == null ? null : v.trim(); }
}
