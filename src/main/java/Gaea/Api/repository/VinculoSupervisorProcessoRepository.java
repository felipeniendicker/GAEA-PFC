package Gaea.Api.repository;

import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.VinculoSupervisorProcesso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface VinculoSupervisorProcessoRepository
        extends JpaRepository<VinculoSupervisorProcesso, Long> {

    boolean existsByProcessoIdAndSupervisorIdAndSituacaoAndAtivoTrue(
            Long processoId,
            Long supervisorId,
            SituacaoVinculoSupervisor situacao);

    boolean existsByProcessoIdAndSituacaoAndAtivoTrue(
            Long processoId,
            SituacaoVinculoSupervisor situacao);

    Optional<VinculoSupervisorProcesso> findByProcessoId(Long processoId);

    Optional<VinculoSupervisorProcesso> findFirstByProcessoIdOrderByCriadoEmDescIdDesc(Long processoId);

    List<VinculoSupervisorProcesso> findAllByEmailConvidadoIgnoreCaseAndSituacaoOrderByCriadoEmAsc(
            String email,
            SituacaoVinculoSupervisor situacao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VinculoSupervisorProcesso v where v.id = :id")
    Optional<VinculoSupervisorProcesso> findByIdParaAtualizacao(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select v from VinculoSupervisorProcesso v
            where v.processo.id = :processoId and v.ativo = true
            """)
    Optional<VinculoSupervisorProcesso> findAtivoByProcessoIdParaAtualizacao(
            @Param("processoId") Long processoId);
}
