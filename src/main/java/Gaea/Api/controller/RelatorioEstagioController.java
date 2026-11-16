package Gaea.Api.controller;

import Gaea.Api.dto.RelatorioEstagioResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.RelatorioEstagioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios/estagios")
public class RelatorioEstagioController {

    private final RelatorioEstagioService service;

    public RelatorioEstagioController(RelatorioEstagioService service) {
        this.service = service;
    }

    @GetMapping
    public RelatorioEstagioResponse gerar(
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String aluno,
            Authentication authentication) {
        return service.gerar(
                (Usuario) authentication.getPrincipal(),
                situacao, dataInicio, dataFim, aluno);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) String aluno,
            Authentication authentication) {
        RelatorioEstagioResponse relatorio = service.gerar(
                (Usuario) authentication.getPrincipal(),
                situacao, dataInicio, dataFim, aluno);
        byte[] conteudo = service.gerarCsv(relatorio);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("relatorio-estagios.csv", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(conteudo);
    }
}
