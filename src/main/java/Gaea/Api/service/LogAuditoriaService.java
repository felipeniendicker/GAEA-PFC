package Gaea.Api.service;

import Gaea.Api.model.LogAuditoria;
import Gaea.Api.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogAuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public LogAuditoriaService(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    public void registrar(String usuario, String acao, String recurso) {
        LogAuditoria log = new LogAuditoria(usuario, acao, recurso);
        logAuditoriaRepository.save(log);
    }

    public List<LogAuditoria> listarTodos() {
        return logAuditoriaRepository.findAll();
    }
}