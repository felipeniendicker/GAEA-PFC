package Gaea.Api.dto;

import Gaea.Api.model.Notificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
        Long id,
        String tipoEvento,
        String titulo,
        String mensagem,
        LocalDateTime criadaEm,
        boolean lida,
        LocalDateTime lidaEm,
        Long processoId) {

    public static NotificacaoResponse from(Notificacao notificacao) {
        return new NotificacaoResponse(
                notificacao.getId(),
                notificacao.getTipoEvento(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                notificacao.getCriadaEm(),
                notificacao.isLida(),
                notificacao.getLidaEm(),
                notificacao.getProcessoId());
    }
}
