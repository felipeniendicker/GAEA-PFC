package Gaea.Api.dto;
import jakarta.validation.constraints.*;
public record DecisaoSolicitacaoInstituicaoRequest(
        @NotBlank(message="Informe a justificativa") @Size(min=10,max=1000) String justificativa) {}
