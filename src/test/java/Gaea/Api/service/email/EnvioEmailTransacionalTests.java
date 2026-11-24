package Gaea.Api.service.email;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvioEmailTransacionalTests {

    @Test
    void devePreservarAssuntosTextosEDespachoDosFluxosExistentes() {
        List<EnvioCapturado> envios = new ArrayList<>();
        EnvioEmailTransacional envio = new EnvioEmailTransacional(
                (destinatario, assunto, texto) ->
                        envios.add(new EnvioCapturado(destinatario, assunto, texto)));

        envio.enviarVerificacao("supervisor@example.com", "Supervisor", "https://gaea/verificar");
        envio.enviarRecuperacaoSenha("usuario@example.com", "Usuário", "https://gaea/redefinir");
        envio.enviarVerificacaoInstituicao("instituicao@example.com", "Responsável", "https://gaea/confirmar");
        envio.enviarAtivacaoInstituicao("instituicao@example.com", "Responsável", "https://gaea/ativar");

        assertTrue(envio.disponivel());
        assertEquals(List.of(
                new EnvioCapturado(
                        "supervisor@example.com",
                        "Confirme seu e-mail no GAEA",
                        "Olá, Supervisor.\n\nConfirme seu e-mail acessando:\nhttps://gaea/verificar"
                                + "\n\nO link é pessoal, temporário e de uso único."),
                new EnvioCapturado(
                        "usuario@example.com",
                        "Redefinição de senha do GAEA",
                        "Olá, Usuário.\n\nRecebemos uma solicitação para redefinir sua senha:\n"
                                + "https://gaea/redefinir\n\nO link expira em 15 minutos e pode ser usado uma única vez."
                                + "\nSe você não fez esta solicitação, ignore esta mensagem."),
                new EnvioCapturado(
                        "instituicao@example.com",
                        "Confirme a solicitação institucional no GAEA",
                        "Olá, Responsável.\n\nConfirme o e-mail institucional:\nhttps://gaea/confirmar"
                                + "\n\nApós a confirmação, a solicitação poderá ser analisada."),
                new EnvioCapturado(
                        "instituicao@example.com",
                        "Ative o acesso institucional ao GAEA",
                        "Olá, Responsável.\n\nA instituição foi aprovada. Defina sua senha e aceite os termos:\n"
                                + "https://gaea/ativar\n\nO convite é pessoal, temporário e de uso único.")), envios);
    }

    private record EnvioCapturado(String destinatario, String assunto, String texto) {
    }
}
