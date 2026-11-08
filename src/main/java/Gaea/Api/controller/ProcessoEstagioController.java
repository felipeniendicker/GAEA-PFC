package Gaea.Api.controller;

import Gaea.Api.dto.CadastroProcessoRequest;
import Gaea.Api.dto.HistoricoStatusResponse;
import Gaea.Api.dto.HistoricoProcessoResponse;
import Gaea.Api.dto.CadastroPendenciaRequest;
import Gaea.Api.dto.PendenciaProcessoResponse;
import Gaea.Api.dto.AtualizacaoPrazoRequest;
import Gaea.Api.dto.CadastroPrazoRequest;
import Gaea.Api.dto.PrazoProcessoResponse;
import Gaea.Api.dto.ProcessoEstagioResponse;
import Gaea.Api.dto.ReaberturaProcessoRequest;
import Gaea.Api.dto.AlteracaoSupervisorRequest;
import Gaea.Api.model.ProcessoEstagio;
import Gaea.Api.service.ProcessoEstagioMapper;
import Gaea.Api.service.HistoricoProcessoService;
import Gaea.Api.service.ProcessoEstagioService;
import Gaea.Api.service.PendenciaProcessoService;
import Gaea.Api.service.PrazoProcessoService;
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
    private final PendenciaProcessoService pendenciaService;
    private final PrazoProcessoService prazoService;
    private final HistoricoProcessoService historicoProcessoService;

    public ProcessoEstagioController(
            ProcessoEstagioService service,
            ProcessoEstagioMapper mapper,
            SupervisaoService supervisaoService,
            PendenciaProcessoService pendenciaService,
            PrazoProcessoService prazoService,
            HistoricoProcessoService historicoProcessoService) {

        this.service = service;
        this.mapper = mapper;
        this.supervisaoService = supervisaoService;
        this.pendenciaService = pendenciaService;
        this.prazoService = prazoService;
        this.historicoProcessoService = historicoProcessoService;
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

    @GetMapping("/{id}/historico-completo")
    public List<HistoricoProcessoResponse> listarHistoricoCompleto(
            @PathVariable Long id,
            Authentication authentication) {

        return historicoProcessoService.listar(
                id,
                (Usuario) authentication.getPrincipal());
    }

    @PostMapping("/{id}/pendencias")
    public PendenciaProcessoResponse registrarPendencia(
            @PathVariable Long id,
            @Valid @RequestBody CadastroPendenciaRequest request,
            Authentication authentication) {

        return pendenciaService.registrar(
                id, request, (Usuario) authentication.getPrincipal());
    }

    @GetMapping("/{id}/pendencias")
    public List<PendenciaProcessoResponse> listarPendencias(
            @PathVariable Long id,
            Authentication authentication) {

        return pendenciaService.listar(id, (Usuario) authentication.getPrincipal());
    }

    @PutMapping("/{id}/pendencias/{pendenciaId}/resolver")
    public PendenciaProcessoResponse resolverPendencia(
            @PathVariable Long id,
            @PathVariable Long pendenciaId,
            Authentication authentication) {

        return pendenciaService.resolver(
                id, pendenciaId, (Usuario) authentication.getPrincipal());
    }

    @PostMapping("/{id}/prazos")
    public PrazoProcessoResponse registrarPrazo(
            @PathVariable Long id,
            @Valid @RequestBody CadastroPrazoRequest request,
            Authentication authentication) {

        return prazoService.registrar(id, request, (Usuario) authentication.getPrincipal());
    }

    @GetMapping("/{id}/prazos")
    public List<PrazoProcessoResponse> listarPrazos(
            @PathVariable Long id,
            Authentication authentication) {

        return prazoService.listar(id, (Usuario) authentication.getPrincipal());
    }

    @PutMapping("/{id}/prazos/{prazoId}")
    public PrazoProcessoResponse atualizarPrazo(
            @PathVariable Long id,
            @PathVariable Long prazoId,
            @Valid @RequestBody AtualizacaoPrazoRequest request,
            Authentication authentication) {

        return prazoService.atualizar(
                id, prazoId, request, (Usuario) authentication.getPrincipal());
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
