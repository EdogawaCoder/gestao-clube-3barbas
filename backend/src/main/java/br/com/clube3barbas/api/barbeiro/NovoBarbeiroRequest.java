package br.com.clube3barbas.api.barbeiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovoBarbeiroRequest(
        @NotBlank(message = "O nome do barbeiro e obrigatorio.")
        @Size(max = 120, message = "O nome do barbeiro e muito longo.")
        String nome
) {
}
