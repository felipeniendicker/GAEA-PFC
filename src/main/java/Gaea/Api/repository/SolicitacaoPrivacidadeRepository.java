package Gaea.Api.repository;

import Gaea.Api.model.SolicitacaoPrivacidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoPrivacidadeRepository
        extends JpaRepository<SolicitacaoPrivacidade, Long> {

    List<SolicitacaoPrivacidade> findAllByUsuarioIdOrderByCriadoEmDescIdDesc(Long usuarioId);
}
