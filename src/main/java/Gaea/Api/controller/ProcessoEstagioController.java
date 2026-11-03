package Gaea.Api.controller;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.dto.HistoricoStatusResponse;
import Gaea.Api.dto.ProcessoEstagioResponse;
import Gaea.Api.dto.ReaberturaProcessoRequest;
import Gaea.Api.dto.AlteracaoSupervisorRequest;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.service.ProcessoEstagioMapper;
import Gaea.Api.service.ProcessoEstagioService;
import Gaea.Api.service.SupervisaoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import Gaea.Api.model.Usuario;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/processos")
public class ProcessoEstagioController {

    private final ProcessoEstagioService service;
    private final ProcessoEstagioMapper mapper;
    private final SupervisaoService supervisaoService;

    public ProcessoEstagioController(
            ProcessoEstagioService service,
            ProcessoEstagioMapper mapper,
            SupervisaoService supervisaoService) {

        this.service = service;
        this.mapper = mapper;
        this.supervisaoService = supervisaoService;
    }

    @PostMapping
    public ProcessoEstagioResponse cadastrar(
            @Valid @RequestBody CadastroProcessoRequest processo,
            Authentication authentication) {

        Usuario usuario = (Usuario) authentication.getPrincipal();
        ProcessoEstagio processoSalvo = service.cadastrar(processo, usuario);

        return mapper.toResponse(processoSalvo);
    }

    @GetMapping
    public List<ProcessoEstagioResponse> listar(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return service.listarParaUsuario(usuario).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/meus")
    public List<ProcessoEstagioResponse> listarMeus(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return service.listarParaUsuario(usuario).stream().map(mapper::toResponse).toList();
    }

    @PutMapping("/{id}/status")
    public ProcessoEstagioResponse alterarStatus(
            @PathVariable Long id,
            @RequestBody String novoStatus,
            Authentication authentication) {

        ProcessoEstagio processoAtualizado =
                service.alterarStatus(
                        id,
                        novoStatus,
                        (Usuario) authentication.getPrincipal());

        return mapper.toResponse(processoAtualizado);
    }

    @PostMapping("/{id}/reabrir")
    public ProcessoEstagioResponse reabrir(
            @PathVariable Long id,
            @Valid @RequestBody ReaberturaProcessoRequest request,
            Authentication authentication) {

        ProcessoEstagio processoAtualizado = service.reabrir(
                id,
                request,
                (Usuario) authentication.getPrincipal());
        return mapper.toResponse(processoAtualizado);
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoStatusResponse> listarHistorico(
            @PathVariable Long id,
            Authentication authentication) {

        return service.listarHistorico(
                id,
                (Usuario) authentication.getPrincipal());
    }

    @PutMapping("/{id}/supervisor")
    public java.util.Map<String, String> alterarSupervisor(
            @PathVariable Long id,
            @Valid @RequestBody AlteracaoSupervisorRequest request,
            Authentication authentication) {
        supervisaoService.alterarSupervisor(
                id, request, (Usuario) authentication.getPrincipal());
        return java.util.Map.of("mensagem", "Nova solicitação de supervisão criada");
    }
}
