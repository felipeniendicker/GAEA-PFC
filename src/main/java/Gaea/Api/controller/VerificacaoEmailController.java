package Gaea.Api.controller;

import Gaea.Api.dto.ConfirmacaoEmailRequest;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.VerificacaoEmailService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/verificacoes-email")
public class VerificacaoEmailController {
    private final VerificacaoEmailService service;

    public VerificacaoEmailController(VerificacaoEmailService service) {
        this.service = service;
    }

    @PostMapping("/confirmar")
    public Map<String, String> confirmar(@Valid @RequestBody ConfirmacaoEmailRequest request) {
        service.confirmar(request);
        return Map.of("mensagem", "E-mail verificado com sucesso");
    }

    @PostMapping("/reenviar")
    public Map<String, String> reenviar(Authentication authentication) {
        service.gerar((Usuario) authentication.getPrincipal());
        return Map.of("mensagem", "Se o SMTP estiver configurado, um novo link será enviado");
    }
}
