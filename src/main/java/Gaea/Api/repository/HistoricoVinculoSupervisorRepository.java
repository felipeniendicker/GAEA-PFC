package Gaea.Api.repository;

import Gaea.Api.model.HistoricoVinculoSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoVinculoSupervisorRepository
        extends JpaRepository<HistoricoVinculoSupervisor, Long> {
    List<HistoricoVinculoSupervisor> findAllByVinculoIdOrderByDataHoraAscIdAsc(Long vinculoId);

    List<HistoricoVinculoSupervisor> findAllByVinculoProcessoIdOrderByDataHoraAscIdAsc(
            Long processoId);
}
