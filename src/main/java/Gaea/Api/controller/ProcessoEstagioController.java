package Gaea.Api.controller;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.dto.HistoricoStatusResponse;
import Gaea.Api.dto.ProcessoEstagioResponse;
import Gaea.Api.dto.ReaberturaProcessoRequest;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.service.LogAuditoriaService;
import Gaea.Api.service.ProcessoEstagioMapper;
import Gaea.Api.service.ProcessoEstagioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import Gaea.Api.model.Usuario;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/processos")
public class ProcessoEstagioController {

    private final ProcessoEstagioService service;
    private final LogAuditoriaService logAuditoriaService;
    private final ProcessoEstagioMapper mapper;

    public ProcessoEstagioController(
            ProcessoEstagioService service,
            LogAuditoriaService logAuditoriaService,
            ProcessoEstagioMapper mapper) {

        this.service = service;
        this.logAuditoriaService = logAuditoriaService;
        this.mapper = mapper;
    }

    @PostMapping
    public ProcessoEstagioResponse cadastrar(
            @Valid @RequestBody CadastroProcessoRequest processo,
            Authentication authentication) {

        ProcessoEstagio processoSalvo = service.cadastrar(processo);

        Usuario usuario = (Usuario) authentication.getPrincipal();
        logAuditoriaService.registrar(
                usuario.getEmail(),
                "CADASTRO_PROCESSO",
                "Processo de estágio " + processoSalvo.getId(),
                usuario.getInstituicao()
        );

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
}
