package Gaea.Api.dto;

import Gaea.Api.model.SolicitacaoInstituicao;
import java.time.LocalDateTime;

public record SolicitacaoInstituicaoResponse(Long id, String nomeInstitucional, String cnpj,
        String responsavelNome, String emailInstitucional, String status, boolean emailVerificado,
        String justificativa, LocalDateTime criadaEm) {
    public static SolicitacaoInstituicaoResponse from(SolicitacaoInstituicao s) {
        return new SolicitacaoInstituicaoResponse(s.getId(), s.getNomeInstitucional(), s.getCnpj(),
                s.getResponsavelNome(), s.getEmailInstitucional(), s.getStatus().name(),
                s.getEmailVerificadoEm() != null, s.getJustificativa(), s.getCriadaEm());
    }
}
