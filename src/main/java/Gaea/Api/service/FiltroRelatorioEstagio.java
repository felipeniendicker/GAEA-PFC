package Gaea.Api.service;

import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.model.ProcessoEstagio;

import java.time.LocalDate;
import java.util.List;

final class FiltroRelatorioEstagio {
    static final List<String> SITUACOES = List.of(
            "PENDENTE", "EM_ANALISE", "APROVADO", "REPROVADO");

    private final String situacao;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;
    private final String aluno;
    private final String empresa;

    FiltroRelatorioEstagio(
            String situacao,
            LocalDate dataInicio,
            LocalDate dataFim,
            String aluno,
            String empresa) {
        validarPeriodo(dataInicio, dataFim);
        this.situacao = normalizarSituacao(situacao);
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.aluno = normalizarTexto(aluno);
        this.empresa = normalizarTexto(empresa);
    }

    boolean corresponde(ProcessoEstagio processo) {
        return correspondeSituacao(processo)
                && correspondePeriodo(processo)
                && correspondeAluno(processo)
                && correspondeEmpresa(processo);
    }

    private boolean correspondeSituacao(ProcessoEstagio processo) {
        return situacao == null || situacao.equals(processo.getStatus());
    }

    private boolean correspondePeriodo(ProcessoEstagio processo) {
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

    private boolean correspondeAluno(ProcessoEstagio processo) {
        return aluno == null
                || (processo.getNomeAluno() != null
                && processo.getNomeAluno().toLowerCase().contains(aluno));
    }

    private boolean correspondeEmpresa(ProcessoEstagio processo) {
        return empresa == null
                || (processo.getNomeEmpresa() != null
                && processo.getNomeEmpresa().toLowerCase().contains(empresa));
    }

    private static String normalizarSituacao(String situacao) {
        String normalizada = normalizarTexto(situacao);
        if (normalizada != null && !SITUACOES.contains(normalizada.toUpperCase())) {
            throw new DadosInvalidosException("Situação de processo inválida");
        }
        return normalizada == null ? null : normalizada.toUpperCase();
    }

    private static String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim().toLowerCase();
    }

    private static void validarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null && dataInicio.isAfter(dataFim)) {
            throw new DadosInvalidosException("O período inicial não pode ser posterior ao final");
        }
    }
}
