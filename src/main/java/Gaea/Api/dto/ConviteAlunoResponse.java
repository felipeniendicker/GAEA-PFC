package Gaea.Api.dto;

import java.time.LocalDateTime;

public record ConviteAlunoResponse(
        AlunoInstituicaoResponse aluno,
        String tokenAtivacao,
        LocalDateTime expiraEm) {

    @Override
    public String toString() {
        return "ConviteAlunoResponse[aluno=" + aluno
                + ", tokenAtivacao=[PROTEGIDO], expiraEm=" + expiraEm + "]";
    }
}
