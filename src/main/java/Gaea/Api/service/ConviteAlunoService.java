package Gaea.Api.service;

import Gaea.Api.dto.AtivacaoAlunoRequest;
import Gaea.Api.exception.ConflitoDadosException;
import Gaea.Api.exception.DadosInvalidosException;
import Gaea.Api.model.ConviteAtivacaoAluno;
import Gaea.Api.model.Usuario;
import Gaea.Api.repository.ConviteAtivacaoAlunoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ConviteAlunoService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ConviteAtivacaoAlunoRepository conviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogAuditoriaService logAuditoriaService;
    private final long expiracaoHoras;

    public ConviteAlunoService(
            ConviteAtivacaoAlunoRepository conviteRepository,
            PasswordEncoder passwordEncoder,
            LogAuditoriaService logAuditoriaService,
            @Value("${app.convite-aluno.expiracao-horas:48}") long expiracaoHoras) {

        this.conviteRepository = conviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.logAuditoriaService = logAuditoriaService;
        this.expiracaoHoras = expiracaoHoras;
    }

    @Transactional
    public ConviteGerado gerar(Usuario aluno) {
        LocalDateTime agora = LocalDateTime.now();
        conviteRepository.findAllByUsuarioIdAndUtilizadoEmIsNullAndCanceladoEmIsNull(aluno.getId())
                .forEach(convite -> convite.setCanceladoEm(agora));

        String token = novoToken();
        ConviteAtivacaoAluno convite = new ConviteAtivacaoAluno();
        convite.setUsuario(aluno);
        convite.setInstituicao(aluno.getInstituicao());
        convite.setEmail(aluno.getEmail());
        convite.setTokenHash(hash(token));
        convite.setCriadoEm(agora);
        convite.setExpiraEm(agora.plusHours(expiracaoHoras));
        conviteRepository.save(convite);

        return new ConviteGerado(token, convite.getExpiraEm());
    }

    @Transactional
    public void ativar(AtivacaoAlunoRequest request) {
        LocalDateTime agora = LocalDateTime.now();
        ConviteAtivacaoAluno convite = conviteRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new DadosInvalidosException("Convite de ativação inválido"));

        if (convite.getUtilizadoEm() != null) {
            throw new DadosInvalidosException("Este convite de ativação já foi utilizado");
        }
        if (convite.getCanceladoEm() != null) {
            throw new DadosInvalidosException("Este convite de ativação não é mais válido");
        }
        if (!convite.getExpiraEm().isAfter(agora)) {
            throw new DadosInvalidosException("Este convite de ativação expirou");
        }

        Usuario aluno = convite.getUsuario();
        if (Boolean.TRUE.equals(aluno.getAtivado())) {
            throw new ConflitoDadosException("A conta deste aluno já está ativada");
        }
        if (!Boolean.TRUE.equals(aluno.getAtivo())) {
            throw new ConflitoDadosException("O aluno está desativado pela instituição");
        }
        if (!aluno.getEmail().equalsIgnoreCase(convite.getEmail())
                || !aluno.getInstituicao().getId().equals(convite.getInstituicao().getId())) {
            throw new DadosInvalidosException("O convite não corresponde ao cadastro do aluno");
        }

        aluno.setSenha(passwordEncoder.encode(request.senha()));
        aluno.setAtivado(true);
        aluno.setTermosAceitos(true);
        aluno.setTermosAceitosEm(agora);
        convite.setUtilizadoEm(agora);

        logAuditoriaService.registrar(
                aluno.getEmail(),
                "ATIVACAO_ALUNO",
                "Ativação da conta do aluno " + aluno.getId(),
                aluno.getInstituicao());
    }

    public ConviteAtivacaoAluno conviteMaisRecente(Long alunoId) {
        return conviteRepository.findFirstByUsuarioIdOrderByCriadoEmDescIdDesc(alunoId).orElse(null);
    }

    private String novoToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        if (token == null || token.isBlank()) {
            throw new DadosInvalidosException("Convite de ativação inválido");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Não foi possível proteger o token de ativação", exception);
        }
    }

    public record ConviteGerado(String token, LocalDateTime expiraEm) {
    }
}
