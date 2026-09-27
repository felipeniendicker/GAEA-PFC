package Gaea.Api.service;

import Gaea.Api.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String CHAVE =
            "gaea-chave-jwt-projeto-academico-2026-seguranca";

    private SecretKey getChave() {
        return Keys.hmacShaKeyFor(CHAVE.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Usuario usuario) {
        long agora = System.currentTimeMillis();

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("perfil", usuario.getPerfil())
                .issuedAt(new Date(agora))
                .expiration(new Date(agora + 3600000))
                .signWith(getChave())
                .compact();
    }
}