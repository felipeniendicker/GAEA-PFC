package Gaea.Api.service;

import Gaea.Api.dto.RedefinicaoSenhaRequest;
import Gaea.Api.dto.SolicitacaoRecuperacaoSenhaRequest;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.exception.LimiteTentativasException;
import Gaea.Api.exception.ServicoIndisponivelException;
import Gaea.Api.model.SolicitacaoRecuperacaoSenha;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.SolicitacaoRecuperacaoSenhaRepository;
import Gaea.Api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RecuperacaoSenhaService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecuperacaoSenhaService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String MENSAGEM_LIMITE =
            "Muitas solicitações de recuperação. Aguarde antes de tentar novamente";

    private final SolicitacaoRecuperacaoSenhaRepository solicitacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaSenhaService politicaSenha;
    private final EnvioEmailAplicacao envioEmail;
    private final LogAuditoriaService auditoriaService;
    private final String frontendBaseUrl;
    private final byte[] chaveHash;
    private final long expiracaoMinutos;
    private final long intervaloMinimoSegundos;
    private final int limitePorIdentificador;
    private final long janelaIdentificadorMinutos;
    private final int limitePorOrigem;
    private final long janelaOrigemMinutos;

    public RecuperacaoSenhaService(
            SolicitacaoRecuperacaoSenhaRepository solicitacaoRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            PoliticaSenhaService politicaSenha,
            EnvioEmailAplicacao envioEmail,
            LogAuditoriaService auditoriaService,
            @Value("${app.frontend.base-url:http://localhost:5174}") String frontendBaseUrl,
            @Value("${app.password-reset.hash-secret}") String chaveHash,
            @Value("${app.password-reset.expiracao-minutos:15}") long expiracaoMinutos,
            @Value("${app.password-reset.intervalo-minimo-segundos:60}") long intervaloMinimoSegundos,
            @Value("${app.password-reset.limite-identificador:3}") int limitePorIdentificador,
            @Value("${app.password-reset.janela-identificador-minutos:60}") long janelaIdentificadorMinutos,
            @Value("${app.password-reset.limite-origem:10}") int limitePorOrigem,
            @Value("${app.password-reset.janela-origem-minutos:15}") long janelaOrigemMinutos) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.politicaSenha = politicaSenha;
        this.envioEmail = envioEmail;
        this.auditoriaService = auditoriaService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.chaveHash = chaveHash.getBytes(StandardCharsets.UTF_8);
        this.expiracaoMinutos = expiracaoMinutos;
        this.intervaloMinimoSegundos = intervaloMinimoSegundos;
        this.limitePorIdentificador = limitePorIdentificador;
        this.janelaIdentificadorMinutos = janelaIdentificadorMinutos;
        this.limitePorOrigem = limitePorOrigem;
        this.janelaOrigemMinutos = janelaOrigemMinutos;
    }

    @Transactional
    public void solicitar(SolicitacaoRecuperacaoSenhaRequest request, String origem) {
        LocalDateTime agora = LocalDateTime.now();
        String identificadorHash = hmac(request.email());
        String origemHash = hmac(origem == null || origem.isBlank() ? "origem-desconhecida" : origem);
        validarLimites(identificadorHash, origemHash, agora);

        Usuario usuario = usuarioRepository.findByEmailNormalizado(request.email())
                .filter(this::podeRecuperar)
                .orElse(null);

        SolicitacaoRecuperacaoSenha solicitacao = new SolicitacaoRecuperacaoSenha();
        solicitacao.setUsuario(usuario);
        solicitacao.setIdentificadorHash(identificadorHash);
        solicitacao.setOrigemHash(origemHash);
        solicitacao.setCriadoEm(agora);

        if (usuario == null) {
            solicitacaoRepository.save(solicitacao);
            return;
        }

        solicitacaoRepository.findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(
                        usuario.getId())
                .forEach(anterior -> anterior.setCanceladoEm(agora));

        String tokenAberto = novoToken();
        solicitacao.setTokenHash(hashToken(tokenAberto));
        solicitacao.setExpiraEm(agora.plusMinutes(expiracaoMinutos));
        solicitacaoRepository.save(solicitacao);

        String link = frontendBaseUrl + "/#redefinirSenha="
                + URLEncoder.encode(tokenAberto, StandardCharsets.UTF_8);
        if (!envioEmail.disponivel()) {
            LOGGER.warn("Solicitação de recuperação registrada sem envio de e-mail disponível");
            return;
        }
        try {
            envioEmail.enviarRecuperacaoSenha(usuario.getEmail(), usuario.getNome(), link);
        } catch (RuntimeException exception) {
            LOGGER.warn("Falha no envio de e-mail de recuperação; resposta permanece genérica");
        }
    }

    @Transactional
    public void redefinir(RedefinicaoSenhaRequest request) {
        politicaSenha.validar(request.senha());
        LocalDateTime agora = LocalDateTime.now();
        String tokenHash = hashToken(request.token());

        Long usuarioId = solicitacaoRepository.findUsuarioIdByTokenHash(tokenHash)
                .orElseThrow(this::tokenInvalido);
        Usuario usuario = usuarioRepository.findByIdParaAtualizacao(usuarioId)
                .orElseThrow(this::tokenInvalido);
        SolicitacaoRecuperacaoSenha solicitacao = solicitacaoRepository
                .findByTokenHashParaAtualizacao(tokenHash)
                .orElseThrow(this::tokenInvalido);

        validarToken(solicitacao, usuario, agora);

        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setCredenciaisVersao(versaoCredenciais(usuario) + 1);
        solicitacao.setUtilizadoEm(agora);
        solicitacaoRepository.findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(
                        usuario.getId())
                .stream()
                .filter(item -> !item.getId().equals(solicitacao.getId()))
                .forEach(item -> item.setCanceladoEm(agora));

        auditoriaService.registrar(
                usuario.getEmail(), "REDEFINICAO_SENHA", "Senha redefinida com recuperação segura",
                usuario.getInstituicao());
    }

    private void validarLimites(String identificadorHash, String origemHash, LocalDateTime agora) {
        solicitacaoRepository.findFirstByIdentificadorHashOrderByCriadoEmDesc(identificadorHash)
                .filter(ultima -> ultima.getCriadoEm().isAfter(
                        agora.minusSeconds(intervaloMinimoSegundos)))
                .ifPresent(ultima -> { throw new LimiteTentativasException(MENSAGEM_LIMITE); });

        long porIdentificador = solicitacaoRepository
                .countByIdentificadorHashAndCriadoEmAfter(
                        identificadorHash, agora.minusMinutes(janelaIdentificadorMinutos));
        long porOrigem = solicitacaoRepository
                .countByOrigemHashAndCriadoEmAfter(
                        origemHash, agora.minusMinutes(janelaOrigemMinutos));

        if (porIdentificador >= limitePorIdentificador || porOrigem >= limitePorOrigem) {
            throw new LimiteTentativasException(MENSAGEM_LIMITE);
        }
    }

    private void validarToken(
            SolicitacaoRecuperacaoSenha solicitacao,
            Usuario usuario,
            LocalDateTime agora) {
        if (solicitacao.getUtilizadoEm() != null) {
            throw new DadosInvalidosException("Este token de recuperação já foi utilizado");
        }
        if (solicitacao.getCanceladoEm() != null) {
            throw new DadosInvalidosException("Este token de recuperação não é mais válido");
        }
        if (solicitacao.getExpiraEm() == null || !solicitacao.getExpiraEm().isAfter(agora)) {
            throw new DadosInvalidosException("Este token de recuperação expirou");
        }
        if (solicitacao.getUsuario() == null
                || !usuario.getId().equals(solicitacao.getUsuario().getId())
                || !podeRecuperar(usuario)) {
            throw tokenInvalido();
        }
    }

    private boolean podeRecuperar(Usuario usuario) {
        return usuario.getSenha() != null
                && Boolean.TRUE.equals(usuario.getAtivo())
                && Boolean.TRUE.equals(usuario.getAtivado())
                && (usuario.getInstituicao() == null
                    || Boolean.TRUE.equals(usuario.getInstituicao().getAtiva()));
    }

    private long versaoCredenciais(Usuario usuario) {
        return usuario.getCredenciaisVersao() == null ? 0L : usuario.getCredenciaisVersao();
    }

    private DadosInvalidosException tokenInvalido() {
        return new DadosInvalidosException("Token de recuperação inválido");
    }

    private String novoToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hmac(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveHash, "HmacSHA256"));
            return HexFormat.of().formatHex(
                    mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger o identificador", exception);
        }
    }

    static String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new DadosInvalidosException("Token de recuperação inválido");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível proteger o token", exception);
        }
    }
}
