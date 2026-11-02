package Gaea.Api.repository;

import Gaea.Api.model.ProcessoEstagio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessoEstagioRepository extends JpaRepository<ProcessoEstagio, Long> {

    List<ProcessoEstagio> findAllByAlunoId(Long alunoId);

    List<ProcessoEstagio> findAllByInstituicaoId(Long instituicaoId);

    @Query("""
            select distinct p
            from ProcessoEstagio p
            join VinculoSupervisorProcesso v on v.processo = p
            where v.supervisor.id = :supervisorId
              and v.situacao = Gaea.Api.model.SituacaoVinculoSupervisor.CONFIRMADO
            """)
    List<ProcessoEstagio> findAllBySupervisorConfirmadoId(
            @Param("supervisorId") Long supervisorId);

    long countByStatus(String status);

    long countByInstituicaoId(Long instituicaoId);

    long countByInstituicaoIdAndStatus(Long instituicaoId, String status);
}
