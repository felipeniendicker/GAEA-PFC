package Gaea.Api.dto;

public record ResumoCargaHorariaResponse(
        long totalRegistradoMinutos,
        long totalAprovadoMinutos,
        long totalPendenteMinutos,
        long totalRejeitadoMinutos) {
}
