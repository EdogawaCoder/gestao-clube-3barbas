package br.com.clube3barbas.api.atendimento;

import jakarta.validation.constraints.NotBlank;

public record NovoAtendimentoRequest(
        @NotBlank(message = "O barbeiro do atendimento e obrigatorio.")
        String barbeiroId
) {
}
