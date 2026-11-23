package Gaea.Api.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DocumentosLegais {

    public static final String VERSAO_TERMOS_USO = "1.0";
    public static final LocalDate PUBLICACAO_TERMOS_USO = LocalDate.of(2026, 9, 27);
    public static final String VERSAO_POLITICA_PRIVACIDADE = "1.0";
    public static final LocalDate PUBLICACAO_POLITICA_PRIVACIDADE = LocalDate.of(2026, 9, 27);

    private DocumentosLegais() {
    }

    public static void registrarAceiteAtual(Usuario usuario, LocalDateTime aceitoEm) {
        usuario.setTermosAceitos(true);
        usuario.setTermosAceitosEm(aceitoEm);
        usuario.setTermosUsoVersao(VERSAO_TERMOS_USO);
        usuario.setPoliticaPrivacidadeVersao(VERSAO_POLITICA_PRIVACIDADE);
    }
}
