package Gaea.Api.controller;

import Gaea.Api.dto.DecisaoPrivacidadeRequest;
import Gaea.Api.dto.SolicitacaoPrivacidadeAdminResponse;
import Gaea.Api.model.SituacaoSolicitacaoPrivacidade;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.SolicitacaoPrivacidadeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/privacidade/solicitacoes")
public class AdminPrivacidadeController {
    private final SolicitacaoPrivacidadeService service;

    public AdminPrivacidadeController(SolicitacaoPrivacidadeService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolicitacaoPrivacidadeAdminResponse> listar(
            @RequestParam(required = false) SituacaoSolicitacaoPrivacidade situacao,
            Authentication authentication) {
        return service.listarAdministracao(
                situacao, (Usuario) authentication.getPrincipal());
    }

    @PutMapping("/{id}/analise")
    public SolicitacaoPrivacidadeAdminResponse analisar(
            @PathVariable Long id,
            Authentication authentication) {
        return service.iniciarAnalise(id, (Usuario) authentication.getPrincipal());
    }

    @PostMapping("/{id}/concluir")
    public SolicitacaoPrivacidadeAdminResponse concluir(
            @PathVariable Long id,
            @Valid @RequestBody DecisaoPrivacidadeRequest request,
            Authentication authentication) {
        return service.concluir(id, request, (Usuario) authentication.getPrincipal());
    }

    @PostMapping("/{id}/rejeitar")
    public SolicitacaoPrivacidadeAdminResponse rejeitar(
            @PathVariable Long id,
            @Valid @RequestBody DecisaoPrivacidadeRequest request,
            Authentication authentication) {
        return service.rejeitar(id, request, (Usuario) authentication.getPrincipal());
    }
}
