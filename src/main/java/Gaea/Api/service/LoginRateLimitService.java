package Gaea.Api.service;

import Gaea.Api.exception.LimiteTentativasException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

@Service
public class LoginRateLimitService {
    private static final String MENSAGEM =
            "Muitas tentativas de autenticação. Aguarde antes de tentar novamente";

    private final Map<String, Deque<Instant>> falhasPorIdentificador = new HashMap<>();
    private final Map<String, Deque<Instant>> falhasPorOrigem = new HashMap<>();
    private final byte[] chaveHash;
    private final int limiteIdentificador;
    private final int limiteOrigem;
    private final int maximoChaves;
    private final Duration janela;
    private final Clock clock;

    @Autowired
    public LoginRateLimitService(
            @Value("${app.login.limite-identificador:5}") int limiteIdentificador,
            @Value("${app.login.limite-origem:20}") int limiteOrigem,
            @Value("${app.login.janela-minutos:15}") long janelaMinutos,
            @Value("${app.login.maximo-chaves:10000}") int maximoChaves,
            @Value("${app.password-reset.hash-secret}") String chaveHash) {
        this(limiteIdentificador, limiteOrigem, maximoChaves,
                Duration.ofMinutes(janelaMinutos),
                chaveHash, Clock.systemUTC());
    }

    LoginRateLimitService(int limiteIdentificador, int limiteOrigem, int maximoChaves,
                          Duration janela, String chaveHash, Clock clock) {
        if (limiteIdentificador < 1 || limiteOrigem < 1 || maximoChaves < 100
                || janela.isZero()
                || janela.isNegative() || chaveHash == null || chaveHash.isBlank()) {
            throw new IllegalArgumentException("Configuração do limite de login inválida");
        }
        this.limiteIdentificador = limiteIdentificador;
        this.limiteOrigem = limiteOrigem;
        this.maximoChaves = maximoChaves;
        this.janela = janela;
        this.chaveHash = chaveHash.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public synchronized void verificar(String emailNormalizado, String origem) {
        Instant agora = clock.instant();
        String identificadorHash = proteger("email:" + emailNormalizado);
        String origemHash = proteger("origem:" + normalizarOrigem(origem));
        if (quantidadeAtual(falhasPorIdentificador, identificadorHash, agora)
                >= limiteIdentificador
                || quantidadeAtual(falhasPorOrigem, origemHash, agora) >= limiteOrigem) {
            throw new LimiteTentativasException(MENSAGEM);
        }
    }

    public synchronized void registrarFalha(String emailNormalizado, String origem) {
        Instant agora = clock.instant();
        adicionar(falhasPorIdentificador, proteger("email:" + emailNormalizado), agora);
        adicionar(falhasPorOrigem, proteger("origem:" + normalizarOrigem(origem)), agora);
    }

    public synchronized void registrarSucesso(String emailNormalizado) {
        falhasPorIdentificador.remove(proteger("email:" + emailNormalizado));
    }

    private void adicionar(Map<String, Deque<Instant>> mapa, String chave, Instant agora) {
        garantirCapacidade(mapa, chave, agora);
        Deque<Instant> falhas = mapa.computeIfAbsent(chave, ignorada -> new ArrayDeque<>());
        removerExpiradas(falhas, agora);
        falhas.addLast(agora);
    }

    private void garantirCapacidade(
            Map<String, Deque<Instant>> mapa, String chave, Instant agora) {
        if (mapa.containsKey(chave) || mapa.size() < maximoChaves) {
            return;
        }
        mapa.entrySet().removeIf(entry -> {
            removerExpiradas(entry.getValue(), agora);
            return entry.getValue().isEmpty();
        });
        if (mapa.size() >= maximoChaves) {
            throw new LimiteTentativasException(MENSAGEM);
        }
    }

    private int quantidadeAtual(Map<String, Deque<Instant>> mapa, String chave, Instant agora) {
        Deque<Instant> falhas = mapa.get(chave);
        if (falhas == null) {
            return 0;
        }
        removerExpiradas(falhas, agora);
        if (falhas.isEmpty()) {
            mapa.remove(chave);
            return 0;
        }
        return falhas.size();
    }

    private void removerExpiradas(Deque<Instant> falhas, Instant agora) {
        Instant limite = agora.minus(janela);
        while (!falhas.isEmpty() && !falhas.getFirst().isAfter(limite)) {
            falhas.removeFirst();
        }
    }

    private String normalizarOrigem(String origem) {
        return origem == null || origem.isBlank() ? "desconhecida" : origem.trim();
    }

    private String proteger(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chaveHash, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger o limite de autenticação",
                    exception);
        }
    }
}
