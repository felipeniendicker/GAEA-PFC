package Gaea.Api.service;

import Gaea.Api.model.Instituicao;
import Gaea.Api.model.LogAuditoria;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.LogAuditoriaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogAuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public LogAuditoriaService(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    public void registrar(String usuario, String acao, String recurso) {
        registrar(usuario, acao, recurso, null);
    }

    public void registrar(String usuario, String acao, String recurso, Instituicao instituicao) {
        LogAuditoria log = new LogAuditoria(usuario, acao, recurso, instituicao);
        logAuditoriaRepository.save(log);
    }

    public List<LogAuditoria> listarParaInstituicao(Usuario usuario) {
        if (!"INSTITUICAO".equals(usuario.getPerfil()) || usuario.getInstituicao() == null) {
            throw new AccessDeniedException("Usuário institucional sem instituição vinculada");
        }

        return logAuditoriaRepository.findAllByInstituicaoIdOrderByDataHoraDescIdDesc(
                usuario.getInstituicao().getId());
    }
}
