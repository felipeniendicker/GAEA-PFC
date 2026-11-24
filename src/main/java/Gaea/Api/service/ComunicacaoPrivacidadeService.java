package Gaea.Api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ComunicacaoPrivacidadeService {

    private final EnvioEmailAplicacao envioEmail;
    private final AuditoriaPrivacidadeService auditoria;

    public ComunicacaoPrivacidadeService(
            EnvioEmailAplicacao envioEmail,
            AuditoriaPrivacidadeService auditoria) {
        this.envioEmail = envioEmail;
        this.auditoria = auditoria;
    }

    public void agendarAposCommit(
            Long solicitacaoId,
            String destinatario,
            String nome,
            boolean concluida) {
        Runnable envio = () -> enviar(solicitacaoId, destinatario, nome, concluida);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            envio.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        envio.run();
                    }
                });
    }

    private void enviar(
            Long solicitacaoId,
            String destinatario,
            String nome,
            boolean concluida) {
        try {
            envioEmail.enviarResultadoPrivacidade(destinatario, nome, concluida);
            auditoria.registrarComunicacao(solicitacaoId);
        } catch (RuntimeException exception) {
            auditoria.registrarFalhaComunicacao(solicitacaoId);
        }
    }
}
