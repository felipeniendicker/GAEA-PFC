package Gaea.Api.config;

import Gaea.Api.service.EnvioEmailAplicacao;
import org.springframework.beans.factory.ObjectProvider;
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
    EnvioEmailAplicacao envioEmail(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.email.remetente:}") String remetente,
            @Value("${app.email.provider:smtp}") String provider,
            @Value("${app.email.brevo-api-key:}") String brevoApiKey) {
        TransporteEmail transporte = switch (provider) {
            case "smtp" -> {
                JavaMailSender smtp = mailSender.getObject();
                yield (destinatario, assunto, texto) -> enviarSmtp(smtp, remetente, destinatario, assunto, texto);
            }
            case "brevo" -> new EnvioBrevo(brevoApiKey, remetente)::enviar;
            default -> throw new IllegalStateException("EMAIL_PROVIDER deve ser smtp ou brevo");
        };
        return criarEnvio(transporte);
    }

    private static EnvioEmailAplicacao criarEnvio(TransporteEmail transporte) {
        return new EnvioEmailAplicacao() {
            @Override
            public boolean disponivel() {
                return true;
            }

            @Override
            public void enviarVerificacao(String destinatario, String nome, String link) {
                transporte.enviar(destinatario, "Confirme seu e-mail no GAEA",
                        "Olá, " + nome + ".\n\nConfirme seu e-mail acessando:\n" + link
                                + "\n\nO link é pessoal, temporário e de uso único.");
            }

            @Override
            public void enviarRecuperacaoSenha(String destinatario, String nome, String link) {
                transporte.enviar(destinatario, "Redefinição de senha do GAEA",
                        "Olá, " + nome + ".\n\nRecebemos uma solicitação para redefinir sua senha:\n"
                                + link + "\n\nO link expira em 15 minutos e pode ser usado uma única vez."
                                + "\nSe você não fez esta solicitação, ignore esta mensagem.");
            }

            @Override
            public void enviarVerificacaoInstituicao(String destinatario, String nome, String link) {
                transporte.enviar(destinatario, "Confirme a solicitação institucional no GAEA",
                        "Olá, " + nome + ".\n\nConfirme o e-mail institucional:\n" + link
                                + "\n\nApós a confirmação, a solicitação poderá ser analisada.");
            }

            @Override
            public void enviarAtivacaoInstituicao(String destinatario, String nome, String link) {
                transporte.enviar(destinatario, "Ative o acesso institucional ao GAEA",
                        "Olá, " + nome + ".\n\nA instituição foi aprovada. Defina sua senha e aceite os termos:\n"
                                + link + "\n\nO convite é pessoal, temporário e de uso único.");
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(EnvioEmailAplicacao.class)
    EnvioEmailAplicacao envioDesabilitado() {
        return new EnvioEmailAplicacao() {
            @Override
            public boolean disponivel() {
                return false;
            }

            @Override
            public void enviarVerificacao(String destinatario, String nome, String link) {
                // A verificação existente continua falhando fechada quando o e-mail está desabilitado.
            }

            @Override
            public void enviarRecuperacaoSenha(String destinatario, String nome, String link) {
                throw new IllegalStateException("O envio de e-mail está desabilitado");
            }
        };
    }

    private static void enviarSmtp(
            JavaMailSender mailSender,
            String remetente,
            String destinatario,
            String assunto,
            String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        if (!remetente.isBlank()) {
            mensagem.setFrom(remetente);
        }
        mensagem.setTo(destinatario);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        mailSender.send(mensagem);
    }

    @FunctionalInterface
    private interface TransporteEmail {
        void enviar(String destinatario, String assunto, String texto);
    }
}
