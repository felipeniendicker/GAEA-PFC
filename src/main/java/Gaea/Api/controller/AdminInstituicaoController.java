package Gaea.Api.controller;

import Gaea.Api.dto.*;
import Gaea.Api.model.*;
import Gaea.Api.service.SolicitacaoInstituicaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin/instituicoes")
public class AdminInstituicaoController {
    private final SolicitacaoInstituicaoService service;
    public AdminInstituicaoController(SolicitacaoInstituicaoService service) { this.service=service; }
    @GetMapping("/dashboard") public Map<String,Long> dashboard() { return service.resumo(); }
    @GetMapping public List<InstituicaoAdminResponse> instituicoes() { return service.listarInstituicoes(); }
    @GetMapping("/solicitacoes") public List<SolicitacaoInstituicaoResponse> solicitacoes(@RequestParam(required=false) StatusSolicitacaoInstituicao status) { return service.listar(status); }
    @PutMapping("/solicitacoes/{id}/analise") public SolicitacaoInstituicaoResponse analisar(@PathVariable Long id, Authentication auth) { return service.iniciarAnalise(id, (Usuario)auth.getPrincipal()); }
    @PostMapping("/solicitacoes/{id}/aprovar") public SolicitacaoInstituicaoResponse aprovar(@PathVariable Long id,
            @Valid @RequestBody AprovacaoInstituicaoRequest request, Authentication auth) {
        return service.aprovar(id, request, (Usuario)auth.getPrincipal());
    }
    @PostMapping("/solicitacoes/{id}/rejeitar") public SolicitacaoInstituicaoResponse rejeitar(@PathVariable Long id, @Valid @RequestBody DecisaoSolicitacaoInstituicaoRequest request, Authentication auth) { return service.rejeitar(id, request, (Usuario)auth.getPrincipal()); }
}
