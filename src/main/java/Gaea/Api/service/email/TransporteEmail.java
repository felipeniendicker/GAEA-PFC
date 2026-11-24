package Gaea.Api.service.email;

@FunctionalInterface
public interface TransporteEmail {
    void enviar(String destinatario, String assunto, String texto);
}
