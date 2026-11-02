package Gaea.Api.controller;

import Gaea.Api.dto.AtivacaoAlunoRequest;
import Gaea.Api.service.ConviteAlunoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ativacoes/aluno")
public class AtivacaoAlunoController {

    private final ConviteAlunoService conviteService;

    public AtivacaoAlunoController(ConviteAlunoService conviteService) {
        this.conviteService = conviteService;
    }

    @PostMapping
    public ResponseEntity<Void> ativar(@Valid @RequestBody AtivacaoAlunoRequest request) {
        conviteService.ativar(request);
        return ResponseEntity.noContent().build();
    }
}
