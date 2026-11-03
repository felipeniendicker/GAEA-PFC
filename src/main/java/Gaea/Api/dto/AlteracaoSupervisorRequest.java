package Gaea.Api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record AlteracaoSupervisorRequest(
        @NotBlank(message = "O e-mail do novo supervisor é obrigatório")
        @Email(message = "Informe um e-mail de supervisor válido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String emailSupervisor) {

    public AlteracaoSupervisorRequest {
        emailSupervisor = emailSupervisor == null
                ? null
                : emailSupervisor.trim().toLowerCase(Locale.ROOT);
    }
}
