package Gaea.Api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ViaCepConfig {

    @Bean("viaCepRestClient")
    RestClient viaCepRestClient(
            @Value("${app.viacep.base-url:https://viacep.com.br}") String baseUrl,
            @Value("${app.viacep.connect-timeout:3s}") Duration connectTimeout,
            @Value("${app.viacep.read-timeout:5s}") Duration readTimeout) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
