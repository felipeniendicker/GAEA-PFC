package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AprovacaoInstituicaoRequest(
        @NotBlank(message = "Informe a sigla da instituição")
        @Size(min = 2, max = 20, message = "A sigla deve ter entre 2 e 20 caracteres")
        @Pattern(regexp = "^[A-Z0-9.-]{2,20}$",
                message = "A sigla deve conter somente letras maiúsculas, números, ponto ou hífen")
        String sigla,
        @NotNull(message = "Confirme se a instituição é fictícia")
        Boolean ficticia) {
    public AprovacaoInstituicaoRequest {
        sigla = sigla == null ? null : sigla.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
