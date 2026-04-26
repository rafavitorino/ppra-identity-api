package br.com.identityapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerificationRequest(

        @NotBlank(message = "O ID do usuário é obrigatório")
        String idUsuario,

        @NotBlank(message = "O e-mail do responsável é obrigatório")
        @Email(message = "Informe um e-mail válido")
        String emailResponsavel
) {}
