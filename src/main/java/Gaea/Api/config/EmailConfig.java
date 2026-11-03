package Gaea.Api.config;

import Gaea.Api.service.EnvioEmailVerificacao;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class EmailConfig {

    @Bean
    @ConditionalOnProperty(name = "app.email.enabled", havingValue = "true")
    EnvioEmailVerificacao envioSmtp(
            JavaMailSender mailSender,
            @Value("${app.email.remetente:}") String remetente) {
        return (destinatario, nome, link) -> {
            SimpleMailMessage mensagem = new SimpleMailMessage();
            if (!remetente.isBlank()) {
                mensagem.setFrom(remetente);
            }
            mensagem.setTo(destinatario);
            mensagem.setSubject("Confirme seu e-mail no GAEA");
            mensagem.setText("Olá, " + nome + ".\n\nConfirme seu e-mail acessando:\n" + link
                    + "\n\nO link é pessoal, temporário e de uso único.");
            mailSender.send(mensagem);
        };
    }

    @Bean
    @ConditionalOnMissingBean(EnvioEmailVerificacao.class)
    EnvioEmailVerificacao envioDesabilitado() {
        return (destinatario, nome, link) -> {
            // Falha fechada: o token permanece protegido e a conta continua não verificada.
        };
    }
}
