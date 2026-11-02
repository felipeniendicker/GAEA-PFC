package Gaea.Api.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> campos = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error ->
                campos.putIfAbsent(error.getField(), error.getDefaultMessage()));

        exception.getBindingResult().getGlobalErrors().forEach(error ->
                campos.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> campos = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                campos.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return build(HttpStatus.BAD_REQUEST, "Dados inválidos", campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage() {
        return build(
                HttpStatus.BAD_REQUEST,
                "A requisição possui dados inválidos ou campos não permitidos",
                Map.of());
    }

    @ExceptionHandler(DadosInvalidosException.class)
    public ResponseEntity<ApiError> handleDadosInvalidos(DadosInvalidosException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNaoEncontrado(RecursoNaoEncontradoException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflitoDadosException.class)
    public ResponseEntity<ApiError> handleConflito(ConflitoDadosException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegridade() {
        return build(HttpStatus.CONFLICT, "Os dados informados entram em conflito com um registro existente", Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        String mensagem = exception.getReason() == null
                ? "Não foi possível concluir a solicitação"
                : exception.getReason();
        return build(exception.getStatusCode(), mensagem, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected() {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu uma falha inesperada. Tente novamente mais tarde",
                Map.of());
    }

    private ResponseEntity<ApiError> build(
            HttpStatusCode status,
            String mensagem,
            Map<String, String> campos) {

        HttpStatus httpStatus = HttpStatus.valueOf(status.value());
        ApiError error = new ApiError(
                LocalDateTime.now(),
                status.value(),
                httpStatus.getReasonPhrase(),
                mensagem,
                campos);

        return ResponseEntity.status(status).body(error);
    }
}
