package Gaea.Api.service;

import Gaea.Api.exception.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaSenhaServiceTests {
    private final PoliticaSenhaService politica = new PoliticaSenhaService();

    @Test
    void deveAceitarSenhaQueAtendeTodosOsRequisitos() {
        assertDoesNotThrow(() -> politica.validar("Frase segura 2026!"));
        assertDoesNotThrow(() -> politica.validar("Árvore9#"));
    }

    @Test
    void deveRejeitarSenhaCurtaComumOuLongaDemais() {
        assertThrows(DadosInvalidosException.class, () -> politica.validar("curta"));
        assertThrows(DadosInvalidosException.class, () -> politica.validar("Senha@123"));
        assertThrows(DadosInvalidosException.class, () -> politica.validar("Áa1!".repeat(20)));
    }

    @Test
    void deveExigirMaiusculaMinusculaNumeroECaractereEspecial() {
        assertThrows(DadosInvalidosException.class, () -> politica.validar("senha1!a"));
        assertThrows(DadosInvalidosException.class, () -> politica.validar("SENHA1!A"));
        assertThrows(DadosInvalidosException.class, () -> politica.validar("Senha!abc"));
        assertThrows(DadosInvalidosException.class, () -> politica.validar("Senha123"));
    }
}
