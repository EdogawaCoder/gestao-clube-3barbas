package br.com.clube3barbas.api.rateio;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtendimentoRateioRequest(
        @NotBlank(message = "O ID do barbeiro e obrigatorio.")
        @Size(max = 128, message = "O ID do barbeiro e muito longo.")
        String barbeiroId,
        @NotBlank(message = "O nome do barbeiro e obrigatorio.")
        @Size(max = 120, message = "O nome do barbeiro e muito longo.")
        String barbeiroNome
) {
}
