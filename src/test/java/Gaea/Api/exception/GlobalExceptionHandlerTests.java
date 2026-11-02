package Gaea.Api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveRetornarBadRequestParaDadosInvalidos() {
        ResponseEntity<ApiError> response = handler.handleDadosInvalidos(
                new DadosInvalidosException("Status inválido"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Status inválido", response.getBody().mensagem());
    }

    @Test
    void deveRetornarNotFoundParaRecursoInexistente() {
        ResponseEntity<ApiError> response = handler.handleNaoEncontrado(
                new RecursoNaoEncontradoException("Processo não encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void deveRetornarConflictParaConflitoDeDados() {
        ResponseEntity<ApiError> response = handler.handleConflito(
                new ConflitoDadosException("E-mail já cadastrado"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void deveOcultarDetalhesDeFalhaInesperada() {
        ResponseEntity<ApiError> response = handler.handleUnexpected();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().mensagem().contains("Exception"));
    }
}
