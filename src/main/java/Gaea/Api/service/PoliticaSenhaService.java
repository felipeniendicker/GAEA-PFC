package Gaea.Api.service;

import Gaea.Api.exception.DadosInvalidosException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Service
public class PoliticaSenhaService {
    public static final int MINIMO_CARACTERES = 8;
    public static final int MAXIMO_CARACTERES = 64;
    private static final int MAXIMO_BYTES_BCRYPT = 72;

    private static final Set<String> SENHAS_COMUNS = Set.of(
            "12345678", "123456789012", "password", "password1!", "password1234", "password123!", "qwerty123456",
            "senha123456", "senha@123456", "administrador", "administrator",
            "senha@123", "letmein123456", "welcome123456", "bemvindo1234", "gaea12345678");

    public void validar(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new DadosInvalidosException("A senha é obrigatória");
        }

        int caracteres = senha.codePointCount(0, senha.length());
        if (caracteres < MINIMO_CARACTERES || caracteres > MAXIMO_CARACTERES) {
            throw new DadosInvalidosException("A senha deve ter entre 8 e 64 caracteres");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES_BCRYPT) {
            throw new DadosInvalidosException("A senha é muito longa. Use uma senha menor");
        }
        if (senha.codePoints().noneMatch(Character::isUpperCase)) {
            throw new DadosInvalidosException("A senha deve ter pelo menos uma letra maiúscula");
        }
        if (senha.codePoints().noneMatch(Character::isLowerCase)) {
            throw new DadosInvalidosException("A senha deve ter pelo menos uma letra minúscula");
        }
        if (senha.codePoints().noneMatch(Character::isDigit)) {
            throw new DadosInvalidosException("A senha deve ter pelo menos um número");
        }
        if (senha.codePoints().noneMatch(PoliticaSenhaService::caractereEspecial)) {
            throw new DadosInvalidosException("A senha deve ter pelo menos um caractere especial");
        }

        String normalizada = senha.strip().toLowerCase(Locale.ROOT);
        if (SENHAS_COMUNS.contains(normalizada)) {
            throw new DadosInvalidosException(
                    "Escolha uma senha menos comum e que não tenha sido amplamente utilizada");
        }
    }

    private static boolean caractereEspecial(int caractere) {
        return !Character.isLetterOrDigit(caractere) && !Character.isWhitespace(caractere);
    }
}
