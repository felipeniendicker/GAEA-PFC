package Gaea.Api.repository;

import Gaea.Api.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findAllByInstituicaoIdOrderByDataHoraDescIdDesc(Long instituicaoId);

    Optional<LogAuditoria> findFirstByInstituicaoIdAndAcaoAndRecursoOrderByDataHoraAscIdAsc(
            Long instituicaoId,
            String acao,
            String recurso);

    Optional<LogAuditoria> findFirstByInstituicaoIdAndAcaoAndRecursoOrderByDataHoraDescIdDesc(
            Long instituicaoId,
            String acao,
            String recurso);
}
