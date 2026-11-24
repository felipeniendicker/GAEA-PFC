package Gaea.Api.service.email;

public final class TemplatesEmail {

    private TemplatesEmail() {
    }

    public static MensagemEmail verificacao(String nome, String link) {
        return new MensagemEmail(
                "Confirme seu e-mail no GAEA",
                "Olá, " + nome + ".\n\nConfirme seu e-mail acessando:\n" + link
                        + "\n\nO link é pessoal, temporário e de uso único.");
    }

    public static MensagemEmail recuperacaoSenha(String nome, String link) {
        return new MensagemEmail(
                "Redefinição de senha do GAEA",
                "Olá, " + nome + ".\n\nRecebemos uma solicitação para redefinir sua senha:\n"
                        + link + "\n\nO link expira em 15 minutos e pode ser usado uma única vez."
                        + "\nSe você não fez esta solicitação, ignore esta mensagem.");
    }

    public static MensagemEmail verificacaoInstituicao(String nome, String link) {
        return new MensagemEmail(
                "Confirme a solicitação institucional no GAEA",
                "Olá, " + nome + ".\n\nConfirme o e-mail institucional:\n" + link
                        + "\n\nApós a confirmação, a solicitação poderá ser analisada.");
    }

    public static MensagemEmail ativacaoInstituicao(String nome, String link) {
        return new MensagemEmail(
                "Ative o acesso institucional ao GAEA",
                "Olá, " + nome + ".\n\nA instituição foi aprovada. Defina sua senha e aceite os termos:\n"
                        + link + "\n\nO convite é pessoal, temporário e de uso único.");
    }

    public static MensagemEmail resultadoPrivacidade(String nome, boolean concluida) {
        String situacao = concluida ? "concluída" : "rejeitada";
        return new MensagemEmail(
                "Atualização da sua solicitação de privacidade no GAEA",
                "Olá, " + nome + ".\n\nSua solicitação de privacidade foi " + situacao
                        + ".\nA mensagem não inclui dados pessoais ou detalhes da decisão. "
                        + "Consulte o GAEA ou o canal institucional responsável para mais informações.");
    }
}
