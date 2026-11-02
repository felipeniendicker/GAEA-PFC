package Gaea.Api.service;

import Gaea.Api.dto.InstituicaoResumoResponse;
import Gaea.Api.dto.ProcessoEstagioResponse;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import org.springframework.stereotype.Component;

@Component
public class ProcessoEstagioMapper {

    public ProcessoEstagioResponse toResponse(ProcessoEstagio processo) {
        Instituicao instituicao = processo.getInstituicao();
        InstituicaoResumoResponse instituicaoResponse = instituicao == null
                ? null
                : new InstituicaoResumoResponse(instituicao.getId(), instituicao.getNome());

        return new ProcessoEstagioResponse(
                processo.getId(),
                processo.getNomeAluno(),
                processo.getEmailAluno(),
                processo.getNomeEmpresa(),
                processo.getCnpjEmpresa(),
                processo.getDataInicio(),
                processo.getDataFim(),
                processo.getStatus(),
                processo.getAluno() == null ? null : processo.getAluno().getId(),
                instituicaoResponse,
                processo.getVersao());
    }
}
