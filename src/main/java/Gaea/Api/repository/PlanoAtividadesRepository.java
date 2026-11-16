package Gaea.Api.repository;

import Gaea.Api.model.PlanoAtividades;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlanoAtividadesRepository extends JpaRepository<PlanoAtividades, Long> {

    boolean existsByProcessoId(Long processoId);

    Optional<PlanoAtividades> findByProcessoId(Long processoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PlanoAtividades p where p.processo.id = :processoId")
    Optional<PlanoAtividades> findByProcessoIdParaAtualizacao(
            @Param("processoId") Long processoId);
}
