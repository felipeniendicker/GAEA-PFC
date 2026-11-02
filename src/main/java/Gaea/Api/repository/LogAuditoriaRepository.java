package Gaea.Api.repository;

import Gaea.Api.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findAllByInstituicaoIdOrderByDataHoraDescIdDesc(Long instituicaoId);
}
