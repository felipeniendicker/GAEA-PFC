package Gaea.Api.controller;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.dto.HistoricoStatusResponse;
import Gaea.Api.dto.ReaberturaProcessoRequest;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.service.LogAuditoriaService;
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

    public ProcessoEstagioController(
            ProcessoEstagioService service,
            LogAuditoriaService logAuditoriaService) {

        this.service = service;
        this.logAuditoriaService = logAuditoriaService;
    }

    @PostMapping
    public ProcessoEstagio cadastrar(
            @Valid @RequestBody CadastroProcessoRequest processo,
            Authentication authentication) {

        ProcessoEstagio processoSalvo = service.cadastrar(processo);

        logAuditoriaService.registrar(
                ((Usuario) authentication.getPrincipal()).getEmail(),
                "CADASTRO_PROCESSO",
                "Processo de estágio " + processoSalvo.getId()
        );

        return processoSalvo;
    }

    @GetMapping
    public List<ProcessoEstagio> listar() {
        return service.listarTodos();
    }

    @GetMapping("/meus")
    public List<ProcessoEstagio> listarMeus(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return service.listarPorEmailAluno(usuario.getEmail());
    }

    @PutMapping("/{id}/status")
    public ProcessoEstagio alterarStatus(
            @PathVariable Long id,
            @RequestBody String novoStatus,
            Authentication authentication) {

        ProcessoEstagio processoAtualizado =
                service.alterarStatus(
                        id,
                        novoStatus,
                        ((Usuario) authentication.getPrincipal()).getEmail());

        return processoAtualizado;
    }

    @PostMapping("/{id}/reabrir")
    public ProcessoEstagio reabrir(
            @PathVariable Long id,
            @Valid @RequestBody ReaberturaProcessoRequest request,
            Authentication authentication) {

        return service.reabrir(
                id,
                request,
                ((Usuario) authentication.getPrincipal()).getEmail());
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
