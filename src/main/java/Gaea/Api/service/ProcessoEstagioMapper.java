package Gaea.Api.service;

import Gaea.Api.dto.InstituicaoResumoResponse;
import Gaea.Api.dto.ProcessoEstagioResponse;
import Gaea.Api.dto.VinculoSupervisorResumoResponse;
import Gaea.Api.model.Instituicao;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.stereotype.Component;

@Component
public class ProcessoEstagioMapper {

    private final VinculoSupervisorProcessoRepository vinculoRepository;

    public ProcessoEstagioMapper(VinculoSupervisorProcessoRepository vinculoRepository) {
        this.vinculoRepository = vinculoRepository;
    }

    public ProcessoEstagioResponse toResponse(ProcessoEstagio processo) {
        Instituicao instituicao = processo.getInstituicao();
        InstituicaoResumoResponse instituicaoResponse = instituicao == null
                ? null
                : new InstituicaoResumoResponse(instituicao.getId(), instituicao.getNome());
        VinculoSupervisorProcesso vinculo = vinculoRepository
                .findFirstByProcessoIdOrderByCriadoEmDescIdDesc(processo.getId())
                .orElse(null);
        VinculoSupervisorResumoResponse vinculoResponse = vinculo == null
                ? null
                : new VinculoSupervisorResumoResponse(
                        vinculo.getEmailConvidado(), vinculo.getSituacao().name(),
                        vinculo.getJustificativaRecusa());

        return new ProcessoEstagioResponse(
                processo.getId(),
                processo.getNomeAluno(),
                processo.getEmailAluno(),
                processo.getNomeEmpresa(),
                processo.getCnpjEmpresa(),
                processo.getCepEmpresa(),
                processo.getEnderecoEmpresa(),
                processo.getDataInicio(),
                processo.getDataFim(),
                processo.getStatus(),
                processo.getAluno() == null ? null : processo.getAluno().getId(),
                instituicaoResponse,
                vinculoResponse,
                processo.getVersao());
    }
}
