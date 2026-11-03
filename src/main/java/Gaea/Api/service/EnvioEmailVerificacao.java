package Gaea.Api.service;

public interface EnvioEmailVerificacao {
    void enviar(String destinatario, String nome, String linkVerificacao);
}
