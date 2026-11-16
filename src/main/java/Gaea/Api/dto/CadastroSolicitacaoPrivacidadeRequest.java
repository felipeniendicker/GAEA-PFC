package Gaea.Api.dto;

import Gaea.Api.model.TipoSolicitacaoPrivacidade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroSolicitacaoPrivacidadeRequest(
        @NotNull(message = "Informe o tipo da solicitação")
        TipoSolicitacaoPrivacidade tipo,

        @NotBlank(message = "Informe a descrição da solicitação")
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String descricao) {
}
