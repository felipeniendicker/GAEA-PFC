package Gaea.Api.repository;

import Gaea.Api.model.TokenVerificacaoEmail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenVerificacaoEmailRepository extends JpaRepository<TokenVerificacaoEmail, Long> {
    @Query("select t.usuario.id from TokenVerificacaoEmail t where t.tokenHash = :tokenHash")
    Optional<Long> findUsuarioIdByTokenHash(@Param("tokenHash") String tokenHash);

    List<TokenVerificacaoEmail> findAllByUsuarioId(Long usuarioId);

    List<TokenVerificacaoEmail> findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenVerificacaoEmail t where t.tokenHash = :tokenHash")
    Optional<TokenVerificacaoEmail> findByTokenHashParaAtualizacao(
            @Param("tokenHash") String tokenHash);
}
