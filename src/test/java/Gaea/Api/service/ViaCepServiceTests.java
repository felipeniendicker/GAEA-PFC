package Gaea.Api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ViaCepServiceTests {

    private MockRestServiceServer servidor;
    private ViaCepService service;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://viacep.test");
        servidor = MockRestServiceServer.bindTo(builder).build();
        service = new ViaCepService(builder.build());
    }

    @Test
    void deveRetornarEnderecoParaCepValidoSemAcessarInternet() {
        servidor.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withSuccess("""
                        {"cep":"01001-000","logradouro":"Praça da Sé","bairro":"Sé",
                         "localidade":"São Paulo","uf":"SP"}
                        """, MediaType.APPLICATION_JSON));

        var resposta = service.buscarCep("01001-000");

        assertEquals("Praça da Sé", resposta.getLogradouro());
        assertEquals("São Paulo", resposta.getLocalidade());
        assertEquals("SP", resposta.getUf());
        servidor.verify();
    }

    @Test
    void deveRejeitarCepComFormatoInvalidoAntesDaChamadaExterna() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarCep("1234"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        servidor.verify();
    }

    @Test
    void deveInformarQuandoCepNaoForEncontrado() {
        servidor.expect(requestTo("https://viacep.test/ws/99999999/json/"))
                .andRespond(withSuccess("{\"erro\":true}", MediaType.APPLICATION_JSON));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarCep("99999999"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        servidor.verify();
    }

    @Test
    void deveTratarTimeoutDoServicoExternoComoIndisponibilidade() {
        servidor.expect(requestTo("https://viacep.test/ws/01001000/json/"))
                .andRespond(withException(new SocketTimeoutException("timeout simulado")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarCep("01001000"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        servidor.verify();
    }
}
