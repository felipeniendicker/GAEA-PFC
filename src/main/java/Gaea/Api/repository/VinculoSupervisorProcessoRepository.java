package Gaea.Api.repository;

import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.VinculoSupervisorProcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VinculoSupervisorProcessoRepository
        extends JpaRepository<VinculoSupervisorProcesso, Long> {

    boolean existsByProcessoIdAndSupervisorIdAndSituacao(
            Long processoId,
            Long supervisorId,
            SituacaoVinculoSupervisor situacao);

    boolean existsByProcessoIdAndSituacao(
            Long processoId,
            SituacaoVinculoSupervisor situacao);

    Optional<VinculoSupervisorProcesso> findByProcessoId(Long processoId);
}
