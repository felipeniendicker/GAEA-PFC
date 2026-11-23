package Gaea.Api.dto;
import jakarta.validation.constraints.*;
public record AtivacaoInstituicaoRequest(@NotBlank @Size(min=40,max=200) String token,
        @NotBlank @Size(min=8,max=64) String senha, @NotBlank String confirmacaoSenha,
        @NotNull @AssertTrue Boolean termosAceitos) {
    @AssertTrue(message="A confirmação da senha não corresponde")
    public boolean isConfirmacaoValida() { return senha != null && senha.equals(confirmacaoSenha); }
    @Override public String toString() { return "AtivacaoInstituicaoRequest[token=[PROTEGIDO], senha=[PROTEGIDA]]"; }
}
