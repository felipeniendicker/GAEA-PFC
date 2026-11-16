package Gaea.Api.repository;

import Gaea.Api.model.DocumentoProcesso;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentoProcessoRepository extends JpaRepository<DocumentoProcesso, Long> {

    List<DocumentoProcesso> findAllByProcessoIdOrderByEnviadoEmDescIdDesc(Long processoId);

    Optional<DocumentoProcesso> findByIdAndProcessoId(Long id, Long processoId);

    boolean existsByDocumentoAnteriorId(Long documentoAnteriorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d
            from DocumentoProcesso d
            where d.id = :id
              and d.processo.id = :processoId
            """)
    Optional<DocumentoProcesso> findByIdAndProcessoIdParaAtualizacao(
            @Param("id") Long id,
            @Param("processoId") Long processoId);
}
