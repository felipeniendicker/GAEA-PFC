package Gaea.Api.service;

import Gaea.Api.model.ViaCepResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ViaCepService {

    private final RestClient restClient;

    public ViaCepService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://viacep.com.br")
                .build();
    }

    public ViaCepResponse buscarCep(String cep) {

        String cepLimpo = cep.replace("-", "").trim();

        if (!cepLimpo.matches("\\d{8}")) {
            throw new IllegalArgumentException("CEP inválido");
        }

        ViaCepResponse resposta = restClient.get()
                .uri("/ws/{cep}/json/", cepLimpo)
                .retrieve()
                .body(ViaCepResponse.class);

        return resposta;
    }
}