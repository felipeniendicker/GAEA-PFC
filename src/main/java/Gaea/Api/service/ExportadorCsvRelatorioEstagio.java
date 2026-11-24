package Gaea.Api.service;

import Gaea.Api.dto.RelatorioEstagioResponse;
import Gaea.Api.dto.RelatorioProcessoResponse;

import java.nio.charset.StandardCharsets;

final class ExportadorCsvRelatorioEstagio {

    private ExportadorCsvRelatorioEstagio() {
    }

    static byte[] gerar(RelatorioEstagioResponse relatorio) {
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

    private static String campo(Object valor) {
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
