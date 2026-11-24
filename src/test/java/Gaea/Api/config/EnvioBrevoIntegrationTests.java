package Gaea.Api.config;

import Gaea.Api.service.EnvioEmailAplicacao;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EnvioBrevoIntegrationTests {
    private static final String REMETENTE = "gaea@example.test";
    private static final String DESTINATARIO = "supervisor@example.test";
    private static final String CHAVE_FICTICIA = "chave-de-teste";
    private static final String LINK = "https://gaea.example.test/#verificar=token-de-teste";

    private HttpServer servidor;
    private EnvioBrevo brevo;
    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(EmailConfig.class)
            .withPropertyValues("app.email.enabled=true", "app.email.remetente=" + REMETENTE);

    @BeforeEach
    void preparar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.start();
        URI endpoint = URI.create("http://127.0.0.1:" + servidor.getAddress().getPort() + "/v3/smtp/email");
        brevo = new EnvioBrevo(CHAVE_FICTICIA, REMETENTE, HttpClient.newHttpClient(), endpoint);
    }

    @AfterEach
    void encerrar() {
        servidor.stop(0);
    }

    @Test
    void enviaPostComAutenticacaoRemetenteDestinatarioEConteudo() {
        AtomicReference<JsonNode> corpo = new AtomicReference<>();
        AtomicReference<String> metodo = new AtomicReference<>();
        AtomicReference<String> chave = new AtomicReference<>();
        AtomicReference<String> tipo = new AtomicReference<>();
        servidor.createContext("/v3/smtp/email", requisicao -> {
            try (requisicao) {
                metodo.set(requisicao.getRequestMethod());
                chave.set(requisicao.getRequestHeaders().getFirst("api-key"));
                tipo.set(requisicao.getRequestHeaders().getFirst("Content-Type"));
                corpo.set(JsonMapper.builder().build().readTree(requisicao.getRequestBody()));
                requisicao.sendResponseHeaders(201, -1);
            }
        });
        String texto = "Olá, <supervisor>.\n" + LINK;
        brevo.enviar(DESTINATARIO, "Confirme seu e-mail no GAEA", texto);

        assertThat(metodo.get()).isEqualTo("POST");
        assertThat(chave.get()).isEqualTo(CHAVE_FICTICIA);
        assertThat(tipo.get()).isEqualTo("application/json");
        assertThat(corpo.get().at("/sender/email").asString()).isEqualTo(REMETENTE);
        assertThat(corpo.get().at("/to/0/email").asString()).isEqualTo(DESTINATARIO);
        assertThat(corpo.get().get("subject").asString()).isEqualTo("Confirme seu e-mail no GAEA");
        assertThat(corpo.get().get("textContent").asString()).isEqualTo(texto);
        assertThat(corpo.get().get("htmlContent").asString()).contains("&lt;supervisor&gt;", LINK);
    }

    @Test
    void descartaConteudoSensivelDaRespostaDeErro() {
        servidor.createContext("/v3/smtp/email", requisicao -> {
            try (requisicao) {
                requisicao.getRequestBody().readAllBytes();
                byte[] dadosSensiveis = (CHAVE_FICTICIA + LINK).getBytes(StandardCharsets.UTF_8);
                requisicao.sendResponseHeaders(401, dadosSensiveis.length);
                requisicao.getResponseBody().write(dadosSensiveis);
            }
        });
        assertThatThrownBy(() -> brevo.enviar(DESTINATARIO, "Assunto", LINK))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("O Brevo recusou o envio de e-mail (HTTP 401)")
                .hasNoCause();
    }

    @Test
    void selecionaBrevoSemDependerDeSmtp() {
        contexto.withPropertyValues("app.email.provider=brevo", "app.email.brevo-api-key=" + CHAVE_FICTICIA)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(EnvioEmailAplicacao.class);
                    assertThat(ctx.getBean(EnvioEmailAplicacao.class).disponivel()).isTrue();
                    assertThat(ctx).doesNotHaveBean(JavaMailSender.class);
                });
    }

    @Test
    void mantemSmtpComoPadrao() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        contexto.withBean(JavaMailSender.class, () -> smtp).run(ctx -> {
            ctx.getBean(EnvioEmailAplicacao.class).enviarVerificacao(DESTINATARIO, "Supervisor", LINK);
            verify(smtp).send(any(SimpleMailMessage.class));
        });
    }
}
