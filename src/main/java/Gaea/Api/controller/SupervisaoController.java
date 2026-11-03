package Gaea.Api.controller;

import Gaea.Api.dto.RecusaSupervisaoRequest;
import Gaea.Api.dto.SolicitacaoSupervisaoResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.SupervisaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/supervisao/solicitacoes")
public class SupervisaoController {
    private final SupervisaoService service;

    public SupervisaoController(SupervisaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolicitacaoSupervisaoResponse> listar(Authentication authentication) {
        return service.listarPendentes((Usuario) authentication.getPrincipal());
    }

    @PostMapping("/{id}/confirmar")
    public Map<String, String> confirmar(@PathVariable Long id, Authentication authentication) {
        service.confirmar(id, (Usuario) authentication.getPrincipal());
        return Map.of("mensagem", "Vínculo de supervisão confirmado");
    }

    @PostMapping("/{id}/recusar")
    public Map<String, String> recusar(
            @PathVariable Long id,
            @Valid @RequestBody RecusaSupervisaoRequest request,
            Authentication authentication) {
        service.recusar(id, request, (Usuario) authentication.getPrincipal());
        return Map.of("mensagem", "Vínculo de supervisão recusado");
    }
}
