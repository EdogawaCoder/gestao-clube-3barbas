package br.com.clube3barbas.domain.assinante;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Assinante do clube com o valor do plano e os percentuais de rateio ja definidos
 * no cadastro. Nesta primeira entrega ainda nao existe versionamento de plano nem
 * pagamento/renovacao de verdade: cada assinante carrega seu proprio valor/percentuais
 * e a data de inicio do ciclo vigente, usados diretamente no fechamento do rateio
 * (ver RateioClubeService). O inicio do ciclo hoje e editado manualmente pelo Gerente;
 * quando o fluxo real de pagamento existir, ele passa a ser definido pelo pagamento.
 */
public record Assinante(
        String id,
        String nome,
        BigDecimal valorPlano,
        BigDecimal percentualGerencia,
        BigDecimal percentualBarbeiros,
        Instant cicloInicio,
        Instant criadoEm
) {
    private static final BigDecimal CEM = new BigDecimal("100");

    /** Duracao do ciclo de vigencia da assinatura, em dias (docs/decisoes-pendentes.md #5). */
    public static final long DURACAO_CICLO_DIAS = 30;

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
        if (cicloInicio == null) {
            throw new IllegalArgumentException("O inicio do ciclo e obrigatorio.");
        }
        if (criadoEm == null) {
            throw new IllegalArgumentException("A data de cadastro e obrigatoria.");
        }
        nome = nome.trim();
        valorPlano = valorPlano.setScale(2, RoundingMode.HALF_UP);
    }

    /** Fim (exclusivo) do ciclo vigente: [cicloInicio, cicloFim). */
    public Instant cicloFim() {
        return cicloInicio.plus(DURACAO_CICLO_DIAS, ChronoUnit.DAYS);
    }

    /** Se o instante informado cai dentro do ciclo vigente deste assinante. */
    public boolean dentroDoCicloVigente(Instant instante) {
        return !instante.isBefore(cicloInicio) && instante.isBefore(cicloFim());
    }
}
