package Gaea.Api.service;

import Gaea.Api.dto.DashboardResumoResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.ProcessoEstagioRepository;
import org.springframework.security.access.AccessDeniedException;
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
    public DashboardResumoResponse obterResumo(Usuario usuario) {
        if (!"INSTITUICAO".equals(usuario.getPerfil()) || usuario.getInstituicao() == null) {
            throw new AccessDeniedException("Usuário institucional sem instituição vinculada");
        }

        Long instituicaoId = usuario.getInstituicao().getId();
        long totalProcessos = processoRepository.countByInstituicaoId(instituicaoId);
        long pendentes = processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "PENDENTE");
        long emAnalise = processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "EM_ANALISE");
        long aprovados = processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "APROVADO");
        long reprovados = processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "REPROVADO");

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
