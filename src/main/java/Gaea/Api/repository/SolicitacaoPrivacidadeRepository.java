package Gaea.Api.repository;

import Gaea.Api.model.SolicitacaoPrivacidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.TipoSolicitacaoPrivacidade;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoPrivacidadeRepository
        extends JpaRepository<SolicitacaoPrivacidade, Long> {

    List<SolicitacaoPrivacidade> findAllByUsuarioIdOrderByCriadoEmDescIdDesc(Long usuarioId);

    boolean existsByUsuarioIdAndTipoAndSituacaoIn(
            Long usuarioId,
            TipoSolicitacaoPrivacidade tipo,
            List<SituacaoSolicitacaoPrivacidade> situacoes);

    @EntityGraph(attributePaths = {"usuario", "usuario.instituicao", "responsavel"})
    List<SolicitacaoPrivacidade> findAllBySituacaoOrderByCriadoEmAsc(
            SituacaoSolicitacaoPrivacidade situacao);

    @EntityGraph(attributePaths = {"usuario", "usuario.instituicao", "responsavel"})
    List<SolicitacaoPrivacidade> findAllByOrderByCriadoEmDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"usuario", "usuario.instituicao", "responsavel"})
    @Query("select s from SolicitacaoPrivacidade s where s.id = :id")
    Optional<SolicitacaoPrivacidade> findByIdParaAtualizacao(@Param("id") Long id);
}
