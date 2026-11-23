package Gaea.Api.repository;

import Gaea.Api.model.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.*;

public interface SolicitacaoInstituicaoRepository extends JpaRepository<SolicitacaoInstituicao, Long> {
    boolean existsByCnpj(String cnpj);
    boolean existsByEmailInstitucionalIgnoreCase(String email);
    long countByOrigemHashAndCriadaEmAfter(String origemHash, LocalDateTime limite);
    long countByStatus(StatusSolicitacaoInstituicao status);
    List<SolicitacaoInstituicao> findAllByStatusOrderByCriadaEmAsc(StatusSolicitacaoInstituicao status);
    List<SolicitacaoInstituicao> findAllByOrderByCriadaEmDesc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoInstituicao s where s.id=:id")
    Optional<SolicitacaoInstituicao> findByIdParaAtualizacao(@Param("id") Long id);
}
