package Gaea.Api.controller;

import Gaea.Api.dto.AvaliacaoCargaHorariaRequest;
import Gaea.Api.dto.CadastroCargaHorariaRequest;
import Gaea.Api.dto.RegistroCargaHorariaResponse;
import Gaea.Api.dto.ResumoCargaHorariaResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.RegistroCargaHorariaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/processos/{processoId}/cargas-horarias")
public class RegistroCargaHorariaController {

    private final RegistroCargaHorariaService service;

    public RegistroCargaHorariaController(RegistroCargaHorariaService service) {
        this.service = service;
    }

    @PostMapping
    public RegistroCargaHorariaResponse cadastrar(
            @PathVariable Long processoId,
            @Valid @RequestBody CadastroCargaHorariaRequest request,
            Authentication authentication) {
        return service.cadastrar(processoId, request, (Usuario) authentication.getPrincipal());
    }

    @GetMapping
    public List<RegistroCargaHorariaResponse> listar(
            @PathVariable Long processoId,
            Authentication authentication) {
        return service.listar(processoId, (Usuario) authentication.getPrincipal());
    }

    @GetMapping("/resumo")
    public ResumoCargaHorariaResponse resumir(
            @PathVariable Long processoId,
            Authentication authentication) {
        return service.resumir(processoId, (Usuario) authentication.getPrincipal());
    }

    @PutMapping("/{registroId}/avaliacao")
    public RegistroCargaHorariaResponse avaliar(
            @PathVariable Long processoId,
            @PathVariable Long registroId,
            @Valid @RequestBody AvaliacaoCargaHorariaRequest request,
            Authentication authentication) {
        return service.avaliar(
                processoId, registroId, request, (Usuario) authentication.getPrincipal());
    }
}
