package Gaea.Api.service;

import Gaea.Api.dto.RelatorioEstagioResponse;
import Gaea.Api.dto.RelatorioProcessoResponse;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.model.SituacaoVinculoSupervisor;
import Gaea.Api.model.Usuario;
import Gaea.Api.model.VinculoSupervisorProcesso;
import Gaea.Api.repository.ProcessoEstagioRepository;
import Gaea.Api.repository.VinculoSupervisorProcessoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RelatorioEstagioService {

    private static final List<String> SITUACOES = List.of(
            "PENDENTE", "EM_ANALISE", "APROVADO", "REPROVADO");

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
            String aluno) {
        validarUsuarioInstitucional(usuario);
        validarPeriodo(dataInicio, dataFim);

        String situacaoNormalizada = normalizarSituacao(situacao);
        String alunoNormalizado = normalizarTexto(aluno);
        List<RelatorioProcessoResponse> processos = processoRepository
                .findAllByInstituicaoId(usuario.getInstituicao().getId())
                .stream()
                .filter(processo -> correspondeSituacao(processo, situacaoNormalizada))
                .filter(processo -> correspondePeriodo(processo, dataInicio, dataFim))
                .filter(processo -> correspondeAluno(processo, alunoNormalizado))
                .map(this::mapear)
                .sorted(Comparator
                        .comparing(RelatorioProcessoResponse::dataInicio,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RelatorioProcessoResponse::processoId))
                .toList();

        Map<String, Long> porSituacao = new LinkedHashMap<>();
        SITUACOES.forEach(status -> porSituacao.put(status, 0L));
        processos.forEach(processo -> porSituacao.compute(
                processo.situacao(), (chave, quantidade) -> quantidade + 1));
        return new RelatorioEstagioResponse(processos.size(), porSituacao, processos);
    }

    public byte[] gerarCsv(RelatorioEstagioResponse relatorio) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Processo;Aluno;Empresa;Supervisor;Situação;Data de início;Data de fim\n");
        for (RelatorioProcessoResponse processo : relatorio.processos()) {
            csv.append(campo(processo.processoId()))
                    .append(';').append(campo(processo.aluno()))
                    .append(';').append(campo(processo.empresa()))
                    .append(';').append(campo(processo.supervisor()))
                    .append(';').append(campo(processo.situacao()))
                    .append(';').append(campo(processo.dataInicio()))
                    .append(';').append(campo(processo.dataFim()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
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

    private boolean correspondeSituacao(ProcessoEstagio processo, String situacao) {
        return situacao == null || situacao.equals(processo.getStatus());
    }

    private boolean correspondePeriodo(
            ProcessoEstagio processo, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null && dataFim == null) {
            return true;
        }
        if (dataInicio != null && processo.getDataFim() == null) {
            return false;
        }
        if (dataFim != null && processo.getDataInicio() == null) {
            return false;
        }
        return (dataInicio == null || !processo.getDataFim().isBefore(dataInicio))
                && (dataFim == null || !processo.getDataInicio().isAfter(dataFim));
    }

    private boolean correspondeAluno(ProcessoEstagio processo, String aluno) {
        return aluno == null
                || (processo.getNomeAluno() != null
                && processo.getNomeAluno().toLowerCase().contains(aluno));
    }

    private String normalizarSituacao(String situacao) {
        String normalizada = normalizarTexto(situacao);
        if (normalizada != null && !SITUACOES.contains(normalizada.toUpperCase())) {
            throw new DadosInvalidosException("Situação de processo inválida");
        }
        return normalizada == null ? null : normalizada.toUpperCase();
    }

    private String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim().toLowerCase();
    }

    private void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new DadosInvalidosException("O período inicial não pode ser posterior ao final");
        }
    }

    private void validarUsuarioInstitucional(Usuario usuario) {
        if (usuario == null || !"INSTITUICAO".equals(usuario.getPerfil())
                || usuario.getInstituicao() == null
                || usuario.getInstituicao().getId() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Somente usuários institucionais podem consultar relatórios");
        }
    }

    private String campo(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor);
        int primeiroConteudo = 0;
        while (primeiroConteudo < texto.length()
                && Character.isWhitespace(texto.charAt(primeiroConteudo))) {
            primeiroConteudo++;
        }
        if (primeiroConteudo < texto.length()
                && "=+-@".indexOf(texto.charAt(primeiroConteudo)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }
}
