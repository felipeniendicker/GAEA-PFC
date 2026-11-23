package Gaea.Api.controller;

import Gaea.Api.dto.*;
import Gaea.Api.service.SolicitacaoInstituicaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/instituicoes")
public class SolicitacaoInstituicaoController {
    private final SolicitacaoInstituicaoService service;
    public SolicitacaoInstituicaoController(SolicitacaoInstituicaoService service) { this.service=service; }

    @PostMapping("/solicitacoes")
    public ResponseEntity<Map<String,String>> solicitar(@Valid @RequestBody SolicitacaoInstituicaoRequest request, HttpServletRequest http) {
        service.solicitar(request, http.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensagem", "Solicitação registrada. Confirme o e-mail institucional para prosseguir"));
    }
    @PostMapping("/solicitacoes/verificar-email")
    public Map<String,String> verificar(@RequestBody Map<String,String> request) { service.confirmarEmail(request.get("token")); return Map.of("mensagem", "E-mail institucional confirmado"); }
    @PostMapping("/ativacoes")
    public ResponseEntity<Void> ativar(@Valid @RequestBody AtivacaoInstituicaoRequest request) { service.ativar(request); return ResponseEntity.noContent().build(); }
}
