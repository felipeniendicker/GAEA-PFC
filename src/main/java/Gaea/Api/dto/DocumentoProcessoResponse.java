package Gaea.Api.dto;

import Gaea.Api.model.DocumentoProcesso;
import Gaea.Api.model.SituacaoDocumento;

import java.time.LocalDateTime;

public record DocumentoProcessoResponse(
        Long id,
        Long processoId,
        Long documentoAnteriorId,
        String nomeOriginal,
        Long tamanhoBytes,
        SituacaoDocumento situacao,
        LocalDateTime enviadoEm,
        String enviadoPor,
        LocalDateTime analisadoEm,
        String analisadoPor,
        String justificativaReprovacao) {

    public static DocumentoProcessoResponse from(DocumentoProcesso documento) {
        return new DocumentoProcessoResponse(
                documento.getId(),
                documento.getProcesso().getId(),
                documento.getDocumentoAnterior() == null
                        ? null : documento.getDocumentoAnterior().getId(),
                documento.getNomeOriginal(),
                documento.getTamanhoBytes(),
                documento.getSituacao(),
                documento.getEnviadoEm(),
                documento.getEnviadoPor().getNome(),
                documento.getAnalisadoEm(),
                documento.getAnalisadoPor() == null
                        ? null : documento.getAnalisadoPor().getNome(),
                documento.getJustificativaReprovacao());
    }
}
