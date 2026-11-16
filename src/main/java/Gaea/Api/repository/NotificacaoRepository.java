package Gaea.Api.repository;

import Gaea.Api.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findAllByDestinatarioIdOrderByCriadaEmDescIdDesc(Long destinatarioId);

    Optional<Notificacao> findByIdAndDestinatarioId(Long id, Long destinatarioId);

    boolean existsByDestinatarioIdAndChaveIdempotencia(
            Long destinatarioId, String chaveIdempotencia);
}
