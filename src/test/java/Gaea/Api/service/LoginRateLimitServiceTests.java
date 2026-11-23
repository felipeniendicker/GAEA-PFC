package Gaea.Api.service;

import Gaea.Api.exception.LimiteTentativasException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginRateLimitServiceTests {

    private static final String CHAVE = "chave-de-testes-com-tamanho-suficiente-2026";

    @Test
    void deveBloquearPorIdentificadorSemArmazenarValorAberto() {
        LoginRateLimitService service = service(2, 20);

        service.registrarFalha("usuario@example.com", "127.0.0.1");
        service.registrarFalha("usuario@example.com", "127.0.0.2");

        assertThrows(LimiteTentativasException.class,
                () -> service.verificar("usuario@example.com", "127.0.0.3"));
        assertDoesNotThrow(() -> service.verificar("outro@example.com", "127.0.0.3"));
    }

    @Test
    void deveManterLimiteDeOrigemAposSucessoDeUmIdentificador() {
        LoginRateLimitService service = service(10, 2);

        service.registrarFalha("primeiro@example.com", "127.0.0.1");
        service.registrarFalha("segundo@example.com", "127.0.0.1");
        service.registrarSucesso("primeiro@example.com");

        assertThrows(LimiteTentativasException.class,
                () -> service.verificar("terceiro@example.com", "127.0.0.1"));
        assertDoesNotThrow(() -> service.verificar("primeiro@example.com", "127.0.0.2"));
    }

    private LoginRateLimitService service(int limiteIdentificador, int limiteOrigem) {
        return new LoginRateLimitService(
                limiteIdentificador,
                limiteOrigem,
                100,
                Duration.ofMinutes(15),
                CHAVE,
                Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC));
    }
}
