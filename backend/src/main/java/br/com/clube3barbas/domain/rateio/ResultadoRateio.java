package br.com.clube3barbas.domain.rateio;

import java.math.BigDecimal;
import java.util.List;

public record ResultadoRateio(
        BigDecimal valorPlano,
        BigDecimal percentualGerencia,
        BigDecimal percentualBarbeiros,
        BigDecimal valorGerencia,
        BigDecimal fundoBarbeiros,
        BigDecimal valorNaoAlocado,
        int totalAtendimentos,
        List<ParcelaBarbeiro> parcelas
) {
    public ResultadoRateio {
        parcelas = List.copyOf(parcelas);
    }
}

