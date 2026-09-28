package Gaea.Api.service;

import Gaea.Api.model.ViaCepResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ViaCepService {

    private final RestClient restClient;

    public ViaCepService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://viacep.com.br")
                .build();
    }

    public ViaCepResponse buscarCep(String cep) {

        String cepLimpo = cep.replaceAll("[\\s-]", "");

        if (!cepLimpo.matches("\\d{8}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "CEP inválido"
            );
        }

        ViaCepResponse resposta;

        try {
            resposta = restClient.get()
                    .uri("/ws/{cep}/json/", cepLimpo)
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de consulta de CEP indisponível"
            );
        }

        if (resposta == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Serviço de consulta de CEP indisponível"
            );
        }

        if (Boolean.TRUE.equals(resposta.getErro())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "CEP não encontrado"
            );
        }

        return resposta;
    }
}
