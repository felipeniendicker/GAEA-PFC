package Gaea.Api.repository;

import Gaea.Api.model.SolicitacaoRecuperacaoSenha;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitacaoRecuperacaoSenhaRepository
        extends JpaRepository<SolicitacaoRecuperacaoSenha, Long> {

    long countByIdentificadorHashAndCriadoEmAfter(String identificadorHash, LocalDateTime limite);

    long countByOrigemHashAndCriadoEmAfter(String origemHash, LocalDateTime limite);

    Optional<SolicitacaoRecuperacaoSenha>
            findFirstByIdentificadorHashOrderByCriadoEmDesc(String identificadorHash);

    List<SolicitacaoRecuperacaoSenha>
            findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(Long usuarioId);

    @Query("select s.usuario.id from SolicitacaoRecuperacaoSenha s "
            + "where s.tokenHash = :tokenHash")
    Optional<Long> findUsuarioIdByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoRecuperacaoSenha s where s.tokenHash = :tokenHash")
    Optional<SolicitacaoRecuperacaoSenha> findByTokenHashParaAtualizacao(
            @Param("tokenHash") String tokenHash);
}
