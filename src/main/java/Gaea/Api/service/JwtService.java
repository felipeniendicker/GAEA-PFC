package Gaea.Api.service;

import Gaea.Api.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {

    private final String chave;
    private final String emissor;
    private final Duration duracao;

    public JwtService(
            @Value("${jwt.secret}") String chave,
            @Value("${jwt.issuer:gaea-api}") String emissor,
            @Value("${jwt.expiration-minutes:60}") long expiracaoMinutos) {
        if (emissor == null || emissor.isBlank() || expiracaoMinutos < 1) {
            throw new IllegalArgumentException("Configuração JWT inválida");
        }
        this.chave = chave;
        this.emissor = emissor;
        this.duracao = Duration.ofMinutes(expiracaoMinutos);
    }

    private SecretKey getChave() {
        return Keys.hmacShaKeyFor(chave.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Usuario usuario) {
        long agora = System.currentTimeMillis();

        return Jwts.builder()
                .subject(usuario.getEmail())
                .issuer(emissor)
                .claim("perfil", usuario.getPerfil())
                .claim("credenciaisVersao", versaoCredenciais(usuario))
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + duracao.toMillis()))
                .signWith(getChave())
                .compact();
    }

    public String extrairEmail(String token) {
        return extrairDados(token).email();
    }

    public DadosToken extrairDados(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getChave())
                .requireIssuer(emissor)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Object versao = claims.get("credenciaisVersao");
        long credenciaisVersao = versao instanceof Number numero ? numero.longValue() : 0L;
        return new DadosToken(claims.getSubject(), credenciaisVersao);
    }

    private long versaoCredenciais(Usuario usuario) {
        return usuario.getCredenciaisVersao() == null ? 0L : usuario.getCredenciaisVersao();
    }

    public record DadosToken(String email, long credenciaisVersao) {
    }
}
