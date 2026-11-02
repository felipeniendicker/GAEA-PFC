package Gaea.Api.service;

import Gaea.Api.dto.DashboardResumoResponse;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class DashboardService {

    private final ProcessoEstagioRepository processoRepository;

    public DashboardService(ProcessoEstagioRepository processoRepository) {
        this.processoRepository = processoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResumoResponse obterResumo() {
        long totalProcessos = processoRepository.count();
        long pendentes = processoRepository.countByStatus("PENDENTE");
        long emAnalise = processoRepository.countByStatus("EM_ANALISE");
        long aprovados = processoRepository.countByStatus("APROVADO");
        long reprovados = processoRepository.countByStatus("REPROVADO");

        return new DashboardResumoResponse(
                totalProcessos,
                pendentes,
                emAnalise,
                aprovados,
                reprovados,
                calcularPercentualAprovacao(aprovados, reprovados));
    }

    private BigDecimal calcularPercentualAprovacao(long aprovados, long reprovados) {
        long decisoesFinais = aprovados + reprovados;
        if (decisoesFinais == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(aprovados)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(decisoesFinais), 2, RoundingMode.HALF_UP);
    }
}
