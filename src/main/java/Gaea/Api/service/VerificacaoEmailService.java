package Gaea.Api.service;

import Gaea.Api.dto.ConfirmacaoEmailRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.model.TokenVerificacaoEmail;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.TokenVerificacaoEmailRepository;
import Gaea.Api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class VerificacaoEmailService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TokenVerificacaoEmailRepository tokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnvioEmailVerificacao envioEmail;
    private final LogAuditoriaService auditoriaService;
    private final long expiracaoHoras;
    private final String frontendBaseUrl;

    public VerificacaoEmailService(
            TokenVerificacaoEmailRepository tokenRepository,
            UsuarioRepository usuarioRepository,
            EnvioEmailVerificacao envioEmail,
            LogAuditoriaService auditoriaService,
            @Value("${app.verificacao-email.expiracao-horas:24}") long expiracaoHoras,
            @Value("${app.frontend.base-url:http://localhost:5174}") String frontendBaseUrl) {
        this.tokenRepository = tokenRepository;
        this.usuarioRepository = usuarioRepository;
        this.envioEmail = envioEmail;
        this.auditoriaService = auditoriaService;
        this.expiracaoHoras = expiracaoHoras;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public void gerar(Usuario usuario) {
        validarSupervisor(usuario);
        Usuario usuarioBloqueado = usuarioRepository.findByIdParaAtualizacao(usuario.getId())
                .orElseThrow(() -> new DadosInvalidosException("Conta de supervisor inválida"));
        validarSupervisor(usuarioBloqueado);
        if (usuarioBloqueado.getEmailVerificadoEm() != null) {
            throw new ConflitoDadosException("O e-mail desta conta já foi verificado");
        }
        LocalDateTime agora = LocalDateTime.now();
        tokenRepository.findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(
                        usuarioBloqueado.getId())
                .forEach(token -> token.setCanceladoEm(agora));

        String tokenAberto = novoToken();
        TokenVerificacaoEmail token = new TokenVerificacaoEmail();
        token.setUsuario(usuarioBloqueado);
        token.setEmail(usuarioBloqueado.getEmail());
        token.setTokenHash(hash(tokenAberto));
        token.setCriadoEm(agora);
        token.setExpiraEm(agora.plusHours(expiracaoHoras));
        tokenRepository.save(token);

        String link = frontendBaseUrl + "/#verificarEmail="
                + URLEncoder.encode(tokenAberto, StandardCharsets.UTF_8);
        envioEmail.enviar(usuarioBloqueado.getEmail(), usuarioBloqueado.getNome(), link);
    }

    @Transactional
    public void confirmar(ConfirmacaoEmailRequest request) {
        LocalDateTime agora = LocalDateTime.now();
        String tokenHash = hash(request.token());
        Long usuarioId = tokenRepository.findUsuarioIdByTokenHash(tokenHash)
                .orElseThrow(() -> new DadosInvalidosException("Token de verificação inválido"));
        Usuario usuarioBloqueado = usuarioRepository
                .findByIdParaAtualizacao(usuarioId)
                .orElseThrow(() -> new DadosInvalidosException("Token de verificação inválido"));
        TokenVerificacaoEmail token = tokenRepository
                .findByTokenHashParaAtualizacao(tokenHash)
                .orElseThrow(() -> new DadosInvalidosException("Token de verificação inválido"));
        if (token.getUtilizadoEm() != null) {
            throw new DadosInvalidosException("Este token de verificação já foi utilizado");
        }
        if (token.getCanceladoEm() != null) {
            throw new DadosInvalidosException("Este token de verificação não é mais válido");
        }
        if (!token.getExpiraEm().isAfter(agora)) {
            throw new DadosInvalidosException("Este token de verificação expirou");
        }

        validarSupervisor(usuarioBloqueado);
        if (!usuarioBloqueado.getId().equals(token.getUsuario().getId())
                || !usuarioBloqueado.getEmail().equalsIgnoreCase(token.getEmail())) {
            throw new DadosInvalidosException("O token não corresponde ao e-mail atual");
        }
        if (usuarioBloqueado.getEmailVerificadoEm() != null) {
            throw new ConflitoDadosException("O e-mail desta conta já foi verificado");
        }

        usuarioBloqueado.setEmailVerificadoEm(agora);
        token.setUtilizadoEm(agora);
        auditoriaService.registrar(
                usuarioBloqueado.getEmail(), "VERIFICACAO_EMAIL", "E-mail do supervisor verificado");
    }

    public void exigirEmailVerificado(Usuario usuario) {
        validarSupervisor(usuario);
        if (usuario.getEmailVerificadoEm() == null) {
            throw new AccessDeniedException("Verifique seu e-mail antes de acessar solicitações de supervisão");
        }
    }

    private void validarSupervisor(Usuario usuario) {
        if (usuario == null || !"EMPRESA".equals(usuario.getPerfil())) {
            throw new AccessDeniedException("A verificação é exclusiva para Supervisor da Empresa");
        }
    }

    private String novoToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new DadosInvalidosException("Token de verificação inválido");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível proteger o token", exception);
        }
    }
}
