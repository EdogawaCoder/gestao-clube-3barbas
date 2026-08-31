package br.com.clube3barbas.domain.rateio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Soma o rateio de todos os assinantes com pelo menos um atendimento registrado:
 * quanto cada barbeiro deve receber, considerando todos os clientes ja atendidos
 * ate o momento (nao apenas um assinante especifico).
 */
public record ResultadoGeralRateio(
        BigDecimal valorTotalPlanos,
        BigDecimal valorTotalGerencia,
        BigDecimal valorTotalBarbeiros,
        int totalAssinantesAtendidos,
        List<ParcelaBarbeiro> parcelas
) {
    public ResultadoGeralRateio {
        parcelas = List.copyOf(parcelas);
    }
}
