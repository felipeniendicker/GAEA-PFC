package Gaea.Api.service.email;

import Gaea.Api.service.EnvioEmailAplicacao;

public class EnvioEmailTransacional implements EnvioEmailAplicacao {
    private final TransporteEmail transporte;

    public EnvioEmailTransacional(TransporteEmail transporte) {
        this.transporte = transporte;
    }

    @Override
    public boolean disponivel() {
        return true;
    }

    @Override
    public void enviarVerificacao(String destinatario, String nome, String link) {
        enviar(destinatario, TemplatesEmail.verificacao(nome, link));
    }

    @Override
    public void enviarRecuperacaoSenha(String destinatario, String nome, String link) {
        enviar(destinatario, TemplatesEmail.recuperacaoSenha(nome, link));
    }

    @Override
    public void enviarVerificacaoInstituicao(String destinatario, String nome, String link) {
        enviar(destinatario, TemplatesEmail.verificacaoInstituicao(nome, link));
    }

    @Override
    public void enviarAtivacaoInstituicao(String destinatario, String nome, String link) {
        enviar(destinatario, TemplatesEmail.ativacaoInstituicao(nome, link));
    }

    @Override
    public void enviarResultadoPrivacidade(
            String destinatario,
            String nome,
            boolean concluida) {
        enviar(destinatario, TemplatesEmail.resultadoPrivacidade(nome, concluida));
    }

    private void enviar(String destinatario, MensagemEmail mensagem) {
        transporte.enviar(destinatario, mensagem.assunto(), mensagem.texto());
    }
}
