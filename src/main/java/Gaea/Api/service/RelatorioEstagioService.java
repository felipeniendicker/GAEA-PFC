package Gaea.Api.service;

import Gaea.Api.dto.RelatorioEstagioResponse;
import Gaea.Api.dto.RelatorioProcessoResponse;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioEstagioService {

    private final ProcessoEstagioRepository processoRepository;
    private final VinculoSupervisorProcessoRepository vinculoRepository;

    public RelatorioEstagioService(
            ProcessoEstagioRepository processoRepository,
            VinculoSupervisorProcessoRepository vinculoRepository) {
        this.processoRepository = processoRepository;
        this.vinculoRepository = vinculoRepository;
    }

    @Transactional(readOnly = true)
    public RelatorioEstagioResponse gerar(
            Usuario usuario,
            String situacao,
            LocalDate dataInicio,
            LocalDate dataFim,
            String aluno,
            String empresa) {
        validarUsuarioInstitucional(usuario);
        FiltroRelatorioEstagio filtro = new FiltroRelatorioEstagio(
                situacao, dataInicio, dataFim, aluno, empresa);

        List<RelatorioProcessoResponse> processos = processoRepository
                .findAllByInstituicaoId(usuario.getInstituicao().getId())
                .stream()
                .filter(filtro::corresponde)
                .map(this::mapear)
                .sorted(Comparator
                        .comparing(RelatorioProcessoResponse::dataInicio,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RelatorioProcessoResponse::processoId))
                .toList();

        Map<String, Long> porSituacao = new LinkedHashMap<>();
        FiltroRelatorioEstagio.SITUACOES.forEach(status -> porSituacao.put(status, 0L));
        processos.forEach(processo -> porSituacao.compute(
                processo.situacao(), (chave, quantidade) -> quantidade + 1));
        return new RelatorioEstagioResponse(processos.size(), porSituacao, processos);
    }

    public byte[] gerarCsv(RelatorioEstagioResponse relatorio) {
        return ExportadorCsvRelatorioEstagio.gerar(relatorio);
    }

    private RelatorioProcessoResponse mapear(ProcessoEstagio processo) {
        VinculoSupervisorProcesso vinculo = vinculoRepository
                .findFirstByProcessoIdOrderByCriadoEmDescIdDesc(processo.getId())
                .orElse(null);
        String supervisor = vinculo == null
                || vinculo.getSituacao() != SituacaoVinculoSupervisor.CONFIRMADO
                || !Boolean.TRUE.equals(vinculo.getAtivo())
                || vinculo.getSupervisor() == null
                ? null : vinculo.getSupervisor().getNome();
        return new RelatorioProcessoResponse(
                processo.getId(),
                processo.getNomeAluno(),
                processo.getNomeEmpresa(),
                supervisor,
                processo.getStatus(),
                processo.getDataInicio(),
                processo.getDataFim());
    }

    private void validarUsuarioInstitucional(Usuario usuario) {
        if (usuario == null || !"INSTITUICAO".equals(usuario.getPerfil())
                || usuario.getInstituicao() == null
                || usuario.getInstituicao().getId() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente usuários institucionais podem consultar relatórios");
        }
    }

}
