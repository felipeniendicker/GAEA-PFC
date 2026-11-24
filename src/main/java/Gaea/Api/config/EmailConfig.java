package Gaea.Api.config;

import Gaea.Api.service.EnvioEmailAplicacao;
import Gaea.Api.service.email.EnvioEmailDesabilitado;
import Gaea.Api.service.email.EnvioEmailTransacional;
import Gaea.Api.service.email.TransporteEmail;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailConfig {

    @Bean
    @ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
    EnvioEmailAplicacao envioEmail(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.email.remetente:}") String remetente,
            @Value("${app.email.provider:smtp}") String provider,
            @Value("${app.email.brevo-api-key:}") String brevoApiKey) {
        TransporteEmail transporte = switch (provider) {
            case "smtp" -> new EnvioSmtp(mailSender.getObject(), remetente);
            case "brevo" -> new EnvioBrevo(brevoApiKey, remetente)::enviar;
            default -> throw new IllegalStateException("EMAIL_PROVIDER deve ser smtp ou brevo");
        };
        return new EnvioEmailTransacional(transporte);
    }

    @Bean
    @ConditionalOnMissingBean(EnvioEmailAplicacao.class)
    EnvioEmailAplicacao envioDesabilitado() {
        return new EnvioEmailDesabilitado();
    }
}
