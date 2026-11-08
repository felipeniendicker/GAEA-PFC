package Gaea.Api.repository;

import Gaea.Api.model.ProcessoEstagio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ProcessoEstagioRepository extends JpaRepository<ProcessoEstagio, Long> {

    List<ProcessoEstagio> findAllByAlunoId(Long alunoId);

    List<ProcessoEstagio> findAllByInstituicaoId(Long instituicaoId);

    @Query("""
            select distinct p
            from ProcessoEstagio p
            join VinculoSupervisorProcesso v on v.processo = p
            where v.supervisor.id = :supervisorId
              and v.situacao = Gaea.Api.model.SituacaoVinculoSupervisor.CONFIRMADO
              and v.ativo = true
            """)
    List<ProcessoEstagio> findAllBySupervisorConfirmadoId(
            @Param("supervisorId") Long supervisorId);

    long countByStatus(String status);

    long countByInstituicaoId(Long instituicaoId);

    long countByInstituicaoIdAndStatus(Long instituicaoId, String status);

    long countByAlunoId(Long alunoId);

    long countByAlunoIdAndStatus(Long alunoId, String status);

    @Query("""
            select count(distinct p.id)
            from ProcessoEstagio p
            join VinculoSupervisorProcesso v on v.processo = p
            where v.supervisor.id = :supervisorId
              and v.situacao = Gaea.Api.model.SituacaoVinculoSupervisor.CONFIRMADO
              and v.ativo = true
            """)
    long countBySupervisorConfirmadoId(@Param("supervisorId") Long supervisorId);

    @Query("""
            select count(distinct p.id)
            from ProcessoEstagio p
            join VinculoSupervisorProcesso v on v.processo = p
            where v.supervisor.id = :supervisorId
              and v.situacao = Gaea.Api.model.SituacaoVinculoSupervisor.CONFIRMADO
              and v.ativo = true
              and p.status = :status
            """)
    long countBySupervisorConfirmadoIdAndStatus(
            @Param("supervisorId") Long supervisorId,
            @Param("status") String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ProcessoEstagio p where p.id = :id")
    Optional<ProcessoEstagio> findByIdParaAtualizacao(@Param("id") Long id);
}
