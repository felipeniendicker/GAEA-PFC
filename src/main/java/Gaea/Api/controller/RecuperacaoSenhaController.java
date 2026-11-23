package Gaea.Api.controller;

import Gaea.Api.dto.RedefinicaoSenhaRequest;
import Gaea.Api.dto.SolicitacaoRecuperacaoSenhaRequest;
import Gaea.Api.service.RecuperacaoSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/senha")
public class RecuperacaoSenhaController {
    private static final String RESPOSTA_GENERICA =
            "Se o e-mail estiver cadastrado e apto, você receberá as instruções de recuperação";

    private final RecuperacaoSenhaService service;

    public RecuperacaoSenhaController(RecuperacaoSenhaService service) {
        this.service = service;
    }

    @PostMapping("/recuperacao")
    public ResponseEntity<Map<String, String>> solicitar(
            @Valid @RequestBody SolicitacaoRecuperacaoSenhaRequest request,
            HttpServletRequest httpRequest) {
        service.solicitar(request, httpRequest.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of("mensagem", RESPOSTA_GENERICA));
    }

    @PostMapping("/redefinicao")
    public Map<String, String> redefinir(@Valid @RequestBody RedefinicaoSenhaRequest request) {
        service.redefinir(request);
        return Map.of("mensagem", "Senha redefinida com sucesso. Entre novamente no GAEA");
    }
}
