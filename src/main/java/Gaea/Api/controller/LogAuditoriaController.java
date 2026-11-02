package Gaea.Api.controller;

import Gaea.Api.dto.LogAuditoriaResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.LogAuditoriaService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class LogAuditoriaController {

    private final LogAuditoriaService logAuditoriaService;

    public LogAuditoriaController(LogAuditoriaService logAuditoriaService) {
        this.logAuditoriaService = logAuditoriaService;
    }

    @GetMapping
    public List<LogAuditoriaResponse> listar(Authentication authentication) {
        Usuario usuario = (Usuario) authentication.getPrincipal();
        return logAuditoriaService.listarParaInstituicao(usuario).stream()
                .map(LogAuditoriaResponse::from)
                .toList();
    }
}
