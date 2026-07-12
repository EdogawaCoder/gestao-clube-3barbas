package br.com.clube3barbas.api.rateio;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record SimulacaoRateioRequest(
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

        @NotNull(message = "A lista de atendimentos e obrigatoria.")
        @Size(max = 500, message = "A simulacao aceita no maximo 500 atendimentos.")
        List<@NotNull(message = "A lista contem um atendimento invalido.") @Valid AtendimentoRateioRequest> atendimentos
) {
}
