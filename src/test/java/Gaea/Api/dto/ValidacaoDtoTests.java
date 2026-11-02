package Gaea.Api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidacaoDtoTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        validatorFactory.close();
    }

    @Test
    void deveAceitarCadastroValidoDeUsuario() {
        CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                "Ana Souza",
                "  ANA@EXAMPLE.COM  ",
                "senha-segura",
                "EMPRESA",
                true);

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("ana@example.com", request.email());
    }

    @Test
    void deveRejeitarCadastroInvalidoDeUsuario() {
        CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                " ",
                "email-invalido",
                "12345678",
                "INSTITUICAO",
                false);

        Set<ConstraintViolation<CadastroUsuarioRequest>> violacoes = validator.validate(request);

        assertEquals(4, violacoes.size());
    }

    @Test
    void deveRejeitarSenhaCurta() {
        CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                "Ana Souza",
                "ana@example.com",
                "1234567",
                "EMPRESA",
                true);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violacao -> violacao.getPropertyPath().toString().equals("senha")));
    }

    @Test
    void deveRejeitarCadastroPublicoDeAluno() {
        CadastroUsuarioRequest request = new CadastroUsuarioRequest(
                "Ana Souza",
                "ana@example.com",
                "senha-segura",
                "ALUNO",
                true);

        assertTrue(validator.validate(request).stream()
                .anyMatch(violacao -> violacao.getPropertyPath().toString().equals("perfil")));
    }

    @Test
    void deveRejeitarCnpjInvalido() {
        CadastroProcessoRequest request = processoValido(
                "11.111.111/1111-11",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 4, 30));

        assertTrue(validator.validate(request).stream()
                .anyMatch(violacao -> violacao.getPropertyPath().toString().equals("cnpjEmpresa")));
    }

    @Test
    void deveAceitarProcessoValido() {
        CadastroProcessoRequest request = processoValido(
                "11.222.333/0001-81",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2027, 4, 30));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void deveRejeitarCamposObrigatoriosAusentes() {
        CadastroProcessoRequest request = new CadastroProcessoRequest(
                null, null, null, null, null, null);

        assertEquals(6, validator.validate(request).size());
    }

    @Test
    void deveRejeitarDataFinalAnteriorAInicial() {
        CadastroProcessoRequest request = processoValido(
                "11.222.333/0001-81",
                LocalDate.of(2027, 1, 10),
                LocalDate.of(2027, 1, 9));

        assertTrue(validator.validate(request).stream()
                .anyMatch(violacao -> violacao.getMessage().contains("data final")));
    }

    @Test
    void deveRejeitarReaberturaSemJustificativa() {
        ReaberturaProcessoRequest request = new ReaberturaProcessoRequest(" ");

        assertTrue(validator.validate(request).stream()
                .anyMatch(violacao -> violacao.getPropertyPath().toString().equals("justificativa")));
    }

    private CadastroProcessoRequest processoValido(
            String cnpj,
            LocalDate dataInicio,
            LocalDate dataFim) {

        return new CadastroProcessoRequest(
                "Ana Souza",
                "ana@example.com",
                "Empresa Exemplo",
                cnpj,
                dataInicio,
                dataFim);
    }
}
