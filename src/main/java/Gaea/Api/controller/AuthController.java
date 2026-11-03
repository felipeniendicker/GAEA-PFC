package Gaea.Api.controller;

import Gaea.Api.dto.CadastroUsuarioRequest;
import Gaea.Api.dto.LoginRequest;
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
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/auth")
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
    public Map<String, Object> login(@Valid @RequestBody LoginRequest dados) {

        String email = usuarioService.normalizarEmail(dados.email());
        String senha = dados.senha();

        Usuario usuario = usuarioRepository.findByEmailNormalizado(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciais inválidas"
                ));

        if (!Boolean.TRUE.equals(usuario.getAtivo())
                || !Boolean.TRUE.equals(usuario.getAtivado())
                || usuario.getSenha() == null
                || !passwordEncoder.matches(senha, usuario.getSenha())) {

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

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("nome", usuario.getNome());
        resposta.put("email", usuario.getEmail());
        resposta.put("perfil", usuario.getPerfil());
        resposta.put("matricula", usuario.getMatricula());
        resposta.put("instituicao", usuario.getInstituicao() == null
                ? null
                : Map.of("nome", usuario.getInstituicao().getNome()));
        resposta.put("token", token);
        return resposta;
    }
}
