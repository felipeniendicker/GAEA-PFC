package Gaea.Api.service;

public interface EnvioEmailAplicacao {
    boolean disponivel();

    void enviarVerificacao(String destinatario, String nome, String linkVerificacao);

    void enviarRecuperacaoSenha(String destinatario, String nome, String linkRecuperacao);

    default void enviarVerificacaoInstituicao(String destinatario, String nome, String link) {
        throw new IllegalStateException("Envio institucional não configurado");
    }

    default void enviarAtivacaoInstituicao(String destinatario, String nome, String link) {
        throw new IllegalStateException("Envio institucional não configurado");
    }
}
