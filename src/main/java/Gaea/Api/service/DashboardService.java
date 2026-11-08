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
        if (usuario == null || usuario.getPerfil() == null) {
            throw new AccessDeniedException("Perfil sem permissão para consultar o dashboard");
        }

        return switch (usuario.getPerfil()) {
            case "INSTITUICAO" -> resumoInstituicao(usuario);
            case "ALUNO" -> resumoAluno(usuario);
            case "EMPRESA" -> resumoEmpresa(usuario);
            default -> throw new AccessDeniedException(
                    "Perfil sem permissão para consultar o dashboard");
        };
    }

    private DashboardResumoResponse resumoInstituicao(Usuario usuario) {
        if (usuario.getInstituicao() == null || usuario.getInstituicao().getId() == null) {
            throw new AccessDeniedException("Usuário institucional sem instituição vinculada");
        }

        Long instituicaoId = usuario.getInstituicao().getId();
        return criarResumo(
                processoRepository.countByInstituicaoId(instituicaoId),
                processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "PENDENTE"),
                processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "EM_ANALISE"),
                processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "APROVADO"),
                processoRepository.countByInstituicaoIdAndStatus(instituicaoId, "REPROVADO"));
    }

    private DashboardResumoResponse resumoAluno(Usuario usuario) {
        if (usuario.getId() == null) {
            throw new AccessDeniedException("Aluno sem identificação válida");
        }

        Long alunoId = usuario.getId();
        return criarResumo(
                processoRepository.countByAlunoId(alunoId),
                processoRepository.countByAlunoIdAndStatus(alunoId, "PENDENTE"),
                processoRepository.countByAlunoIdAndStatus(alunoId, "EM_ANALISE"),
                processoRepository.countByAlunoIdAndStatus(alunoId, "APROVADO"),
                processoRepository.countByAlunoIdAndStatus(alunoId, "REPROVADO"));
    }

    private DashboardResumoResponse resumoEmpresa(Usuario usuario) {
        if (usuario.getId() == null) {
            throw new AccessDeniedException("Supervisor sem identificação válida");
        }

        Long supervisorId = usuario.getId();
        return criarResumo(
                processoRepository.countBySupervisorConfirmadoId(supervisorId),
                processoRepository.countBySupervisorConfirmadoIdAndStatus(supervisorId, "PENDENTE"),
                processoRepository.countBySupervisorConfirmadoIdAndStatus(supervisorId, "EM_ANALISE"),
                processoRepository.countBySupervisorConfirmadoIdAndStatus(supervisorId, "APROVADO"),
                processoRepository.countBySupervisorConfirmadoIdAndStatus(supervisorId, "REPROVADO"));
    }

    private DashboardResumoResponse criarResumo(
            long totalProcessos,
            long pendentes,
            long emAnalise,
            long aprovados,
            long reprovados) {

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
