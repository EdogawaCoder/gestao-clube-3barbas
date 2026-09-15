package br.com.clube3barbas.api.assinante;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record AtualizarAssinanteRequest(
        @NotBlank(message = "O nome do assinante e obrigatorio.")
        @Size(max = 120, message = "O nome do assinante e muito longo.")
        String nome,

        @NotNull(message = "O valor do plano e obrigatorio.")
        @DecimalMin(value = "0.01", message = "O valor do plano deve ser positivo.")
        @Digits(integer = 10, fraction = 2, message = "O valor do plano deve ter no maximo duas casas decimais.")
        BigDecimal valorPlano,

        @NotNull(message = "O percentual da gerencia e obrigatorio.")
        @DecimalMin(value = "0", message = "O percentual nao pode ser negativo.")
        @DecimalMax(value = "100", message = "O percentual nao pode ultrapassar 100.")
        @Digits(integer = 3, fraction = 4, message = "O percentual deve ter no maximo quatro casas decimais.")
        BigDecimal percentualGerencia,

        @NotNull(message = "O percentual dos barbeiros e obrigatorio.")
        @DecimalMin(value = "0", message = "O percentual nao pode ser negativo.")
        @DecimalMax(value = "100", message = "O percentual nao pode ultrapassar 100.")
        @Digits(integer = 3, fraction = 4, message = "O percentual deve ter no maximo quatro casas decimais.")
        BigDecimal percentualBarbeiros,

        @NotNull(message = "O inicio do ciclo e obrigatorio.")
        Instant cicloInicio
) {
}
