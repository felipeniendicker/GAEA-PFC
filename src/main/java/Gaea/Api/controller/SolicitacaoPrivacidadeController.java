package Gaea.Api.controller;

import Gaea.Api.dto.CadastroSolicitacaoPrivacidadeRequest;
import Gaea.Api.dto.SolicitacaoPrivacidadeResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.SolicitacaoPrivacidadeService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/privacidade/solicitacoes")
public class SolicitacaoPrivacidadeController {

    private final SolicitacaoPrivacidadeService service;

    public SolicitacaoPrivacidadeController(SolicitacaoPrivacidadeService service) {
        this.service = service;
    }

    @PostMapping
    public SolicitacaoPrivacidadeResponse cadastrar(
            @Valid @RequestBody CadastroSolicitacaoPrivacidadeRequest request,
            Authentication authentication) {
        return service.cadastrar(
                request, (Usuario) authentication.getPrincipal());
    }

    @GetMapping
    public List<SolicitacaoPrivacidadeResponse> listar(Authentication authentication) {
        return service.listar((Usuario) authentication.getPrincipal());
    }
}
