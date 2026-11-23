package Gaea.Api.dto;

import Gaea.Api.model.DocumentosLegais;

import java.time.LocalDate;

public record DocumentosLegaisResponse(
        String versaoTermosUso,
        LocalDate termosUsoPublicadoEm,
        String versaoPoliticaPrivacidade,
        LocalDate politicaPrivacidadePublicadaEm) {

    public static DocumentosLegaisResponse atual() {
        return new DocumentosLegaisResponse(
                DocumentosLegais.VERSAO_TERMOS_USO,
                DocumentosLegais.PUBLICACAO_TERMOS_USO,
                DocumentosLegais.VERSAO_POLITICA_PRIVACIDADE,
                DocumentosLegais.PUBLICACAO_POLITICA_PRIVACIDADE);
    }
}
