package Gaea.Api.dto;

import java.time.LocalDateTime;

public record HistoricoProcessoResponse(
        String id,
        String tipo,
        LocalDateTime dataHora,
        String responsavel,
        String descricao) {
}
