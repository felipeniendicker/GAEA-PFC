package Gaea.Api.controller;

import Gaea.Api.dto.NotificacaoResponse;
import Gaea.Api.model.Usuario;
import Gaea.Api.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificacaoResponse> listar(Authentication authentication) {
        return service.listar((Usuario) authentication.getPrincipal());
    }

    @GetMapping("/nao-lidas/contagem")
    public Map<String, Long> contarNaoLidas(Authentication authentication) {
        return Map.of("naoLidas", service.contarNaoLidas(
                (Usuario) authentication.getPrincipal()));
    }

    @PutMapping("/{id}/ler")
    public ResponseEntity<Void> marcarComoLida(
            @PathVariable Long id, Authentication authentication) {
        service.marcarComoLida(id, (Usuario) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
