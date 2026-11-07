package Gaea.Api.repository;

import Gaea.Api.model.PendenciaProcesso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PendenciaProcessoRepository extends JpaRepository<PendenciaProcesso, Long> {

    List<PendenciaProcesso> findAllByProcessoIdOrderByCriadaEmDescIdDesc(Long processoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from PendenciaProcesso p
            where p.id = :id
              and p.processo.id = :processoId
            """)
    Optional<PendenciaProcesso> findByIdAndProcessoIdParaAtualizacao(
            @Param("id") Long id,
            @Param("processoId") Long processoId);
}
