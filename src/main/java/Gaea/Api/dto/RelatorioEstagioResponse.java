package Gaea.Api.dto;

import java.util.List;
import java.util.Map;

public record RelatorioEstagioResponse(
        long total,
        Map<String, Long> porSituacao,
        List<RelatorioProcessoResponse> processos) {
}
