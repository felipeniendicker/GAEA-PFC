package Gaea.Api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CadastroPlanoAtividadesRequest(
        @NotEmpty(message = "Informe ao menos uma atividade prevista")
        List<@Valid AtividadePlanoRequest> atividades,

        @NotNull(message = "Informe a carga horária planejada")
        @DecimalMin(value = "0.01", message = "A carga horária planejada deve ser positiva")
        @Digits(integer = 5, fraction = 2,
                message = "A carga horária deve possuir no máximo duas casas decimais")
        BigDecimal cargaHorariaPlanejada) {
}
