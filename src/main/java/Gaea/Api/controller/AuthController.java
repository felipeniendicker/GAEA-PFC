package Gaea.Api.controller;

import Gaea.Api.dto.CadastroUsuarioRequest;
import Gaea.Api.dto.LoginRequest;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.UsuarioRepository;
import Gaea.Api.service.JwtService;
import Gaea.Api.service.LoginRateLimitService;
import Gaea.Api.service.LogAuditoriaService;
import Gaea.Api.service.UsuarioService;
import Gaea.Api.service.VerificacaoEmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
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
    private final VerificacaoEmailService verificacaoEmailService;
    private final LoginRateLimitService loginRateLimitService;
    private final String senhaInexistenteHash;

    public AuthController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LogAuditoriaService logAuditoriaService,
            UsuarioService usuarioService,
            VerificacaoEmailService verificacaoEmailService,
            LoginRateLimitService loginRateLimitService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.logAuditoriaService = logAuditoriaService;
        this.usuarioService = usuarioService;
        this.verificacaoEmailService = verificacaoEmailService;
        this.loginRateLimitService = loginRateLimitService;
        this.senhaInexistenteHash = passwordEncoder.encode(
                "credencial-inexistente-" + java.util.UUID.randomUUID());
    }

    @PostMapping("/cadastro")
    @Transactional
    public Map<String, String> cadastrar(@Valid @RequestBody CadastroUsuarioRequest dados) {
        verificacaoEmailService.exigirDisponibilidade();
        Usuario usuarioSalvo = usuarioService.cadastrar(dados);
        verificacaoEmailService.gerar(usuarioSalvo);

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
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest dados,
            HttpServletRequest request) {

        String email = usuarioService.normalizarEmail(dados.email());
        String senha = dados.senha();
        String origem = request.getRemoteAddr();
        loginRateLimitService.verificar(email, origem);

        Usuario usuario = usuarioRepository.findByEmailNormalizado(email).orElse(null);
        boolean senhaValida = passwordEncoder.matches(
                senha, usuario == null || usuario.getSenha() == null
                        ? senhaInexistenteHash : usuario.getSenha());

        if (usuario == null
                || !Boolean.TRUE.equals(usuario.getAtivo())
                || !Boolean.TRUE.equals(usuario.getAtivado())
                || !instituicaoAtiva(usuario)
                || usuario.getSenha() == null
                || !senhaValida) {

            loginRateLimitService.registrarFalha(email, origem);

            if (usuario != null) {
                logAuditoriaService.registrar(
                        usuario.getEmail(),
                        "LOGIN_FALHA",
                        "Tentativa de autenticação",
                        usuario.getInstituicao()
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciais inválidas"
            );
        }

        loginRateLimitService.registrarSucesso(email);

        String token = jwtService.gerarToken(usuario);

        logAuditoriaService.registrar(
                usuario.getEmail(),
                "LOGIN",
                "Autenticação no sistema",
                usuario.getInstituicao()
        );

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("nome", usuario.getNome());
        resposta.put("email", usuario.getEmail());
        resposta.put("perfil", usuario.getPerfil());
        resposta.put("emailVerificado", usuario.getEmailVerificadoEm() != null);
        resposta.put("matricula", usuario.getMatricula());
        resposta.put("instituicao", usuario.getInstituicao() == null
                ? null
                : Map.of(
                        "nome", usuario.getInstituicao().getNome(),
                        "sigla", usuario.getInstituicao().getSigla() == null ? "" : usuario.getInstituicao().getSigla()));
        resposta.put("token", token);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(resposta);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        usuarioService.revogarCredenciais((Usuario) authentication.getPrincipal());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private boolean instituicaoAtiva(Usuario usuario) {
        return usuario.getInstituicao() == null
                || Boolean.TRUE.equals(usuario.getInstituicao().getAtiva());
    }
}
