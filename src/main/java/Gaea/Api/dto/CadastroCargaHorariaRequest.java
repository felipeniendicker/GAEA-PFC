package Gaea.Api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CadastroCargaHorariaRequest(
        @NotNull(message = "Informe a data da atividade")
        LocalDate dataAtividade,

        @NotNull(message = "Informe a duração da atividade")
        @Positive(message = "A duração deve ser maior que zero")
        Integer duracaoMinutos,

        @NotBlank(message = "Informe a atividade desenvolvida")
        @Size(max = 1000, message = "A atividade deve ter no máximo 1000 caracteres")
        String atividade) {
}
