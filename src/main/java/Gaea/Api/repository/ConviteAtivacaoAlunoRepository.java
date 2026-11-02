package Gaea.Api.repository;

import Gaea.Api.model.ConviteAtivacaoAluno;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ConviteAtivacaoAlunoRepository extends JpaRepository<ConviteAtivacaoAluno, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConviteAtivacaoAluno> findByTokenHash(String tokenHash);

    List<ConviteAtivacaoAluno> findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(
            Long usuarioId);

    Optional<ConviteAtivacaoAluno> findFirstByUsuarioIdOrderByCriadoEmDescIdDesc(Long usuarioId);
}
