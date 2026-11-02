package Gaea.Api.controller;

import Gaea.Api.dto.AlteracaoSituacaoAlunoRequest;
import Gaea.Api.dto.AlunoInstituicaoResponse;
import Gaea.Api.dto.AtualizacaoAlunoInstituicaoRequest;
import Gaea.Api.dto.CadastroAlunoInstituicaoRequest;
import Gaea.Api.dto.ConviteAlunoResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.AlunoInstituicaoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/instituicoes/alunos")
public class AlunoInstituicaoController {

    private final AlunoInstituicaoService service;

    public AlunoInstituicaoController(AlunoInstituicaoService service) {
        this.service = service;
    }

    @PostMapping
    public ConviteAlunoResponse cadastrar(
            @Valid @RequestBody CadastroAlunoInstituicaoRequest request,
            Authentication authentication) {

        return service.cadastrar(request, usuario(authentication));
    }

    @GetMapping
    public List<AlunoInstituicaoResponse> listar(Authentication authentication) {
        return service.listar(usuario(authentication));
    }

    @PutMapping("/{alunoId}")
    public AlunoInstituicaoResponse atualizar(
            @PathVariable Long alunoId,
            @Valid @RequestBody AtualizacaoAlunoInstituicaoRequest request,
            Authentication authentication) {

        return service.atualizar(alunoId, request, usuario(authentication));
    }

    @PatchMapping("/{alunoId}/situacao")
    public AlunoInstituicaoResponse alterarSituacao(
            @PathVariable Long alunoId,
            @Valid @RequestBody AlteracaoSituacaoAlunoRequest request,
            Authentication authentication) {

        return service.alterarSituacao(alunoId, request.ativo(), usuario(authentication));
    }

    @PostMapping("/{alunoId}/convite")
    public ConviteAlunoResponse reenviarConvite(
            @PathVariable Long alunoId,
            Authentication authentication) {

        return service.reenviarConvite(alunoId, usuario(authentication));
    }

    private Usuario usuario(Authentication authentication) {
        return (Usuario) authentication.getPrincipal();
    }
}
