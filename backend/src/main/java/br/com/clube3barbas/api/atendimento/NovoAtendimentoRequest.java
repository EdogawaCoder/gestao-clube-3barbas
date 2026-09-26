package br.com.clube3barbas.api.atendimento;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/** dataHora e opcional: sem ela, o atendimento fica registrado no instante atual. */
public record NovoAtendimentoRequest(
        @NotBlank(message = "O barbeiro do atendimento e obrigatorio.")
        String barbeiroId,
        Instant dataHora
) {
}
