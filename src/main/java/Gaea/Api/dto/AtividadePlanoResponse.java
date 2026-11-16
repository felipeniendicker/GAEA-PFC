package Gaea.Api.dto;

import Gaea.Api.model.AtividadePlano;

public record AtividadePlanoResponse(Long id, String descricao, Integer ordem) {
    public static AtividadePlanoResponse from(AtividadePlano atividade) {
        return new AtividadePlanoResponse(
                atividade.getId(), atividade.getDescricao(), atividade.getOrdem());
    }
}
