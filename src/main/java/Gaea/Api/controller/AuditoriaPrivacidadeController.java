package Gaea.Api.controller;

import Gaea.Api.dto.EventoPrivacidadeResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.SolicitacaoPrivacidadeService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/privacidade/auditoria")
public class AuditoriaPrivacidadeController {

    private final SolicitacaoPrivacidadeService service;

    public AuditoriaPrivacidadeController(SolicitacaoPrivacidadeService service) {
        this.service = service;
    }

    @GetMapping
    public List<EventoPrivacidadeResponse> listar(
            @RequestParam Long solicitacaoId,
            Authentication authentication) {
        return service.listarAuditoria(
                solicitacaoId, (Usuario) authentication.getPrincipal());
    }
}
