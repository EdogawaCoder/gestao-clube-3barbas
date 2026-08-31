package br.com.clube3barbas.domain.assinante;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Assinante do clube com o valor do plano e os percentuais de rateio ja definidos
 * no cadastro. Nesta primeira entrega ainda nao existe versionamento de plano nem
 * ciclo de pagamento: cada assinante carrega seu proprio valor/percentuais, usados
 * diretamente no fechamento do rateio (ver RateioClubeService).
 */
public record Assinante(
        String id,
        String nome,
        BigDecimal valorPlano,
        BigDecimal percentualGerencia,
        BigDecimal percentualBarbeiros,
        Instant criadoEm
) {
    private static final BigDecimal CEM = new BigDecimal("100");

    public Assinante {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O ID do assinante e obrigatorio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do assinante e obrigatorio.");
        }
        if (valorPlano == null || valorPlano.signum() <= 0) {
            throw new IllegalArgumentException("O valor do plano deve ser maior que zero.");
        }
        if (percentualGerencia == null || percentualBarbeiros == null) {
            throw new IllegalArgumentException("Os percentuais de rateio sao obrigatorios.");
        }
        if (percentualGerencia.signum() < 0 || percentualBarbeiros.signum() < 0) {
            throw new IllegalArgumentException("Os percentuais nao podem ser negativos.");
        }
        if (percentualGerencia.add(percentualBarbeiros).compareTo(CEM) != 0) {
            throw new IllegalArgumentException("Os percentuais devem somar 100%.");
        }
        if (criadoEm == null) {
            throw new IllegalArgumentException("A data de cadastro e obrigatoria.");
        }
        nome = nome.trim();
        valorPlano = valorPlano.setScale(2, RoundingMode.HALF_UP);
    }
}
