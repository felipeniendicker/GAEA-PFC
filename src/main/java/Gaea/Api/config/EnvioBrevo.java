package Gaea.Api.config;

import org.springframework.web.util.HtmlUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

final class EnvioBrevo {
    private static final URI ENDPOINT = URI.create("https://api.brevo.com/v3/smtp/email");
    private static final Duration TIMEOUT_RESPOSTA = Duration.ofSeconds(5);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final String apiKey;
    private final String remetente;
    private final HttpClient cliente;
    private final URI endpoint;

    EnvioBrevo(String apiKey, String remetente) {
        this(apiKey, remetente, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(), ENDPOINT);
    }

    EnvioBrevo(String apiKey, String remetente, HttpClient cliente, URI endpoint) {
        if (apiKey.isBlank() || remetente.isBlank()) {
            throw new IllegalStateException("BREVO_API_KEY e SMTP_FROM são obrigatórios com EMAIL_PROVIDER=brevo");
        }
        this.apiKey = apiKey;
        this.remetente = remetente;
        this.cliente = cliente;
        this.endpoint = endpoint;
    }

    void enviar(String destinatario, String assunto, String texto) {
        try {
            byte[] corpo = JSON.writeValueAsBytes(Map.of(
                    "sender", Map.of("email", remetente),
                    "to", List.of(Map.of("email", destinatario)),
                    "subject", assunto,
                    "textContent", texto,
                    "htmlContent", "<p>" + HtmlUtils.htmlEscape(texto).replace("\n", "<br>") + "</p>"));
            HttpRequest requisicao = HttpRequest.newBuilder(endpoint)
                    .timeout(TIMEOUT_RESPOSTA)
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(corpo))
                    .build();

            HttpResponse<InputStream> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofInputStream());
            // O status confirma o envio. Feche o corpo sem ler dados do provedor.
            resposta.body().close();
            if (resposta.statusCode() != 201) {
                throw new IllegalStateException("O Brevo recusou o envio de e-mail (HTTP " + resposta.statusCode() + ")");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envio de e-mail pelo Brevo interrompido");
        } catch (IOException | JacksonException | IllegalArgumentException exception) {
            throw new IllegalStateException("Falha na comunicação com o Brevo");
        }
    }
}
