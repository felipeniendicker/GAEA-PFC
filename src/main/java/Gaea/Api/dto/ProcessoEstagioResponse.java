package Gaea.Api.dto;

import java.time.LocalDate;

public record ProcessoEstagioResponse(
        Long id,
        String nomeAluno,
        String emailAluno,
        String nomeEmpresa,
        String cnpjEmpresa,
        String cepEmpresa,
        String enderecoEmpresa,
        LocalDate dataInicio,
        LocalDate dataFim,
        String status,
        Long alunoId,
        InstituicaoResumoResponse instituicao,
        VinculoSupervisorResumoResponse vinculoSupervisor,
        Long versao) {
}
