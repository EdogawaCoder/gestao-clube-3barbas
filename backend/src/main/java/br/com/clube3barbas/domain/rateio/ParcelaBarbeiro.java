package br.com.clube3barbas.domain.rateio;

import java.math.BigDecimal;

public record ParcelaBarbeiro(
        String barbeiroId,
        String barbeiroNome,
        int quantidadeAtendimentos,
        BigDecimal percentualDoFundo,
        BigDecimal percentualDoPlano,
        BigDecimal valor
) {
}

