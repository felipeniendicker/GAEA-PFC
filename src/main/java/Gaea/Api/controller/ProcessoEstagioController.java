package Gaea.Api.controller;

import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.service.ProcessoEstagioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/processos")
public class ProcessoEstagioController {

    private final ProcessoEstagioService service;

    public ProcessoEstagioController(ProcessoEstagioService service) {
        this.service = service;
    }

    @PostMapping
    public ProcessoEstagio cadastrar(@RequestBody ProcessoEstagio processo) {
        return service.cadastrar(processo);
    }

    @GetMapping
    public List<ProcessoEstagio> listar() {
        return service.listarTodos();
    }
    @PutMapping("/{id}/status")
    public ProcessoEstagio alterarStatus(
        @PathVariable Long id,
        @RequestBody String novoStatus) {

        return service.alterarStatus(id, novoStatus);
    }
}