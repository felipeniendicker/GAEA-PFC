package Gaea.Api.dto;
import Gaea.Api.model.Instituicao;
import java.time.LocalDateTime;
public record InstituicaoAdminResponse(Long id, String nome, String cnpj, String sigla,
        boolean ficticia, boolean ativa, LocalDateTime criadaEm) {
    public static InstituicaoAdminResponse from(Instituicao i) {
        return new InstituicaoAdminResponse(i.getId(), i.getNome(), i.getCnpj(), i.getSigla(),
                Boolean.TRUE.equals(i.getFicticia()), Boolean.TRUE.equals(i.getAtiva()), i.getCriadaEm());
    }
}
