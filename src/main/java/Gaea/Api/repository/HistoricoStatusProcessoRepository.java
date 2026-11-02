package Gaea.Api.repository;

import Gaea.Api.model.HistoricoStatusProcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoStatusProcessoRepository
        extends JpaRepository<HistoricoStatusProcesso, Long> {

    List<HistoricoStatusProcesso> findByProcessoIdOrderByDataHoraAscIdAsc(Long processoId);
}
