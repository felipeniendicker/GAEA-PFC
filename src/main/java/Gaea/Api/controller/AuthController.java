package Gaea.Api.controller;

import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import Gaea.Api.service.LogAuditoriaService;
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

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LogAuditoriaService logAuditoriaService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.logAuditoriaService = logAuditoriaService;
    }

    @PostMapping("/cadastro")
    public Map<String, String> cadastrar(@RequestBody Usuario usuario) {

        String perfil = usuario.getPerfil();

        if (!"ALUNO".equals(perfil) && !"EMPRESA".equals(perfil)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Perfil inválido para cadastro público"
            );
        }

        if (!Boolean.TRUE.equals(usuario.getTermosAceitos())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "É necessário aceitar os Termos de Uso e a Política de Privacidade"
            );
        }

        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "E-mail já cadastrado"
            );
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        return Map.of(
                "nome", usuarioSalvo.getNome(),
                "email", usuarioSalvo.getEmail(),
                "perfil", usuarioSalvo.getPerfil()
        );
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> dados) {

        String email = dados.get("email");
        String senha = dados.get("senha");

        Usuario usuario = usuarioRepository.findByEmail(email)
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