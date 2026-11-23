package Gaea.Api.repository;

import Gaea.Api.model.TokenFluxoInstituicao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface TokenFluxoInstituicaoRepository extends JpaRepository<TokenFluxoInstituicao, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenFluxoInstituicao t where t.tokenHash=:hash")
    Optional<TokenFluxoInstituicao> findByTokenHashParaAtualizacao(@Param("hash") String hash);
    List<TokenFluxoInstituicao> findAllBySolicitacaoIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(Long id);
    List<TokenFluxoInstituicao> findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(Long id);
}
