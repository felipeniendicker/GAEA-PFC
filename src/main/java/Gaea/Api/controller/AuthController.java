package Gaea.Api.controller;

import Gaea.Api.dto.CadastroUsuarioRequest;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import Gaea.Api.service.LogAuditoriaService;
import Gaea.Api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LogAuditoriaService logAuditoriaService;
    private final UsuarioService usuarioService;

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LogAuditoriaService logAuditoriaService,
            UsuarioService usuarioService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.logAuditoriaService = logAuditoriaService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/cadastro")
    public Map<String, String> cadastrar(@Valid @RequestBody CadastroUsuarioRequest dados) {
        Usuario usuarioSalvo = usuarioService.cadastrar(dados);

        logAuditoriaService.registrar(
                usuarioSalvo.getEmail(),
                "ACEITE_TERMOS",
                "Termos de Uso e Política de Privacidade"
        );

        return Map.of(
                "nome", usuarioSalvo.getNome(),
                "email", usuarioSalvo.getEmail(),
                "perfil", usuarioSalvo.getPerfil()
        );
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> dados) {

        String email = usuarioService.normalizarEmail(dados.get("email"));
        String senha = dados.get("senha");

        Usuario usuario = usuarioRepository.findByEmailNormalizado(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciais inválidas"
                ));

        if (!passwordEncoder.matches(senha, usuario.getSenha())) {

            logAuditoriaService.registrar(
                    email,
                    "LOGIN_FALHA",
                    "Tentativa de autenticação"
            );

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciais inválidas"
            );
        }

        String token = jwtService.gerarToken(usuario);

        logAuditoriaService.registrar(
                usuario.getEmail(),
                "LOGIN",
                "Autenticação no sistema"
        );

        return Map.of(
                "nome", usuario.getNome(),
                "email", usuario.getEmail(),
                "perfil", usuario.getPerfil(),
                "token", token
        );
    }
}
