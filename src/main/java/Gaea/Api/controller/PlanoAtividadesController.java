package Gaea.Api.controller;

import Gaea.Api.dto.AvaliacaoPlanoAtividadesRequest;
import Gaea.Api.dto.CadastroPlanoAtividadesRequest;
import Gaea.Api.dto.PlanoAtividadesResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.PlanoAtividadesService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/processos/{processoId}/plano-atividades")
public class PlanoAtividadesController {

    private final PlanoAtividadesService service;

    public PlanoAtividadesController(PlanoAtividadesService service) {
        this.service = service;
    }

    @PostMapping
    public PlanoAtividadesResponse cadastrar(
            @PathVariable Long processoId,
            @Valid @RequestBody CadastroPlanoAtividadesRequest request,
            Authentication authentication) {
        return service.cadastrar(
                processoId, request, (Usuario) authentication.getPrincipal());
    }

    @GetMapping
    public PlanoAtividadesResponse consultar(
            @PathVariable Long processoId,
            Authentication authentication) {
        return service.consultar(processoId, (Usuario) authentication.getPrincipal());
    }

    @PutMapping("/avaliacao")
    public PlanoAtividadesResponse avaliar(
            @PathVariable Long processoId,
            @Valid @RequestBody AvaliacaoPlanoAtividadesRequest request,
            Authentication authentication) {
        return service.avaliar(
                processoId, request, (Usuario) authentication.getPrincipal());
    }
}
