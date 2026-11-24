package Gaea.Api.service;

import Gaea.Api.dto.EventoPrivacidadeResponse;
import Gaea.Api.model.LogAuditoria;
import Gaea.Api.repository.LogAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditoriaPrivacidadeService {

    private static final String RECURSO = "Solicitação de privacidade ";
    private final LogAuditoriaRepository repository;

    public AuditoriaPrivacidadeService(LogAuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(String ator, String acao, Long solicitacaoId) {
        repository.save(new LogAuditoria(ator, acao, recurso(solicitacaoId)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalha(String ator, Long solicitacaoId) {
        registrar(ator, "FALHA_EXECUCAO_PRIVACIDADE", solicitacaoId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFalhaComunicacao(Long solicitacaoId) {
        registrar("SISTEMA", "FALHA_COMUNICACAO_PRIVACIDADE", solicitacaoId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarComunicacao(Long solicitacaoId) {
        registrar("SISTEMA", "COMUNICACAO_PRIVACIDADE", solicitacaoId);
    }

    @Transactional(readOnly = true)
    public List<EventoPrivacidadeResponse> listar(Long solicitacaoId) {
        return repository.findAllByRecursoInOrderByDataHoraAscIdAsc(List.of(
                        recurso(solicitacaoId),
                        "Solicitação " + solicitacaoId))
                .stream()
                .map(EventoPrivacidadeResponse::from)
                .toList();
    }

    private String recurso(Long solicitacaoId) {
        return RECURSO + solicitacaoId;
    }
}
