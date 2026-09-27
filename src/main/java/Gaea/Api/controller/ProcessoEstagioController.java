package Gaea.Api.controller;

import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.service.LogAuditoriaService;
import Gaea.Api.service.ProcessoEstagioService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
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
            @RequestBody ProcessoEstagio processo,
            Authentication authentication) {

        ProcessoEstagio processoSalvo = service.cadastrar(processo);

        logAuditoriaService.registrar(
                authentication.getName(),
                "CADASTRO_PROCESSO",
                "Processo de estágio " + processoSalvo.getId()
        );

        return processoSalvo;
    }

    @GetMapping
    public List<ProcessoEstagio> listar() {
        return service.listarTodos();
    }

    @PutMapping("/{id}/status")
    public ProcessoEstagio alterarStatus(
            @PathVariable Long id,
            @RequestBody String novoStatus,
            Authentication authentication) {

        ProcessoEstagio processoAtualizado =
                service.alterarStatus(id, novoStatus);

        logAuditoriaService.registrar(
                authentication.getName(),
                "ALTERACAO_STATUS",
                "Processo de estágio " + id
        );

        return processoAtualizado;
    }
}