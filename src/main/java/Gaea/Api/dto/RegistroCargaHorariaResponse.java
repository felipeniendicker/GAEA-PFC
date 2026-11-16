package Gaea.Api.dto;

import Gaea.Api.model.RegistroCargaHoraria;
import Gaea.Api.model.SituacaoCargaHoraria;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RegistroCargaHorariaResponse(
        Long id,
        Long processoId,
        LocalDate dataAtividade,
        Integer duracaoMinutos,
        String atividade,
        SituacaoCargaHoraria situacao,
        LocalDateTime criadoEm,
        String registradoPor,
        LocalDateTime avaliadoEm,
        String avaliadoPor,
        String justificativaRejeicao) {

    public static RegistroCargaHorariaResponse from(RegistroCargaHoraria registro) {
        return new RegistroCargaHorariaResponse(
                registro.getId(),
                registro.getProcesso().getId(),
                registro.getDataAtividade(),
                registro.getDuracaoMinutos(),
                registro.getAtividade(),
                registro.getSituacao(),
                registro.getCriadoEm(),
                registro.getRegistradoPor().getNome(),
                registro.getAvaliadoEm(),
                registro.getAvaliadoPor() == null ? null : registro.getAvaliadoPor().getNome(),
                registro.getJustificativaRejeicao());
    }
}
