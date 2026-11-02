package Gaea.Api.dto;

import java.time.LocalDateTime;

public record AlunoInstituicaoResponse(
        Long id,
        String nome,
        String email,
        String matricula,
        boolean ativo,
        boolean ativado,
        String situacaoAtivacao,
        LocalDateTime conviteExpiraEm) {
}
