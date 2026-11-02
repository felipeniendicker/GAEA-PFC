package Gaea.Api.dto;

import java.math.BigDecimal;

public record DashboardResumoResponse(
        long totalProcessos,
        long pendentes,
        long emAnalise,
        long aprovados,
        long reprovados,
        BigDecimal percentualAprovacao) {
}
