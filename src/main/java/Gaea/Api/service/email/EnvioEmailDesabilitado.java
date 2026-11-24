package Gaea.Api.service.email;

import Gaea.Api.service.EnvioEmailAplicacao;

public class EnvioEmailDesabilitado implements EnvioEmailAplicacao {

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
}
