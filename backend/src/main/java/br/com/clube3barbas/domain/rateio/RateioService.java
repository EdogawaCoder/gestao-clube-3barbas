package br.com.clube3barbas.domain.rateio;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class RateioService {

    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal UM_CENTAVO = new BigDecimal("0.01");
    private static final int ESCALA_PERCENTUAL = 4;
    private static final int ESCALA_CALCULO = 12;

    /**
     * Executa apenas a aritmetica do rateio. No fechamento real, o chamador deve obter
     * valor pago, percentuais congelados e atendimentos validos do ciclo no servidor;
     * esses dados nunca podem ser aceitos diretamente do navegador.
     */
    public ResultadoRateio calcular(
            BigDecimal valorPlano,
            BigDecimal percentualGerencia,
            BigDecimal percentualBarbeiros,
            List<AtendimentoRateio> atendimentos
    ) {
        validar(valorPlano, percentualGerencia, percentualBarbeiros, atendimentos);

        var valorNormalizado = valorPlano.setScale(2, RoundingMode.HALF_UP);
        var valorGerencia = valorNormalizado
                .multiply(percentualGerencia)
                .divide(CEM, 2, RoundingMode.HALF_UP);
        var fundoBarbeiros = valorNormalizado.subtract(valorGerencia);

        if (atendimentos.isEmpty()) {
            return new ResultadoRateio(
                    valorNormalizado,
                    percentualGerencia,
                    percentualBarbeiros,
                    valorGerencia,
                    fundoBarbeiros,
                    fundoBarbeiros,
                    0,
                    List.of()
            );
        }

        var porBarbeiro = agruparAtendimentos(atendimentos);
        var totalAtendimentos = atendimentos.size();
        var parcelasProvisorias = calcularParcelasProvisorias(
                porBarbeiro,
                totalAtendimentos,
                fundoBarbeiros
        );
        distribuirCentavosResiduais(parcelasProvisorias, fundoBarbeiros);
        var percentuaisDoFundo = distribuirPercentual(porBarbeiro, totalAtendimentos, CEM);
        var percentuaisDoPlano = distribuirPercentual(porBarbeiro, totalAtendimentos, percentualBarbeiros);

        var parcelas = parcelasProvisorias.stream()
                .sorted(Comparator.comparing(ParcelaProvisoria::barbeiroId))
                .map(parcela -> new ParcelaBarbeiro(
                        parcela.barbeiroId(),
                        parcela.barbeiroNome(),
                        parcela.quantidadeAtendimentos(),
                        percentuaisDoFundo.get(parcela.barbeiroId()),
                        percentuaisDoPlano.get(parcela.barbeiroId()),
                        parcela.valorArredondado()
                ))
                .toList();

        return new ResultadoRateio(
                valorNormalizado,
                percentualGerencia,
                percentualBarbeiros,
                valorGerencia,
                fundoBarbeiros,
                BigDecimal.ZERO.setScale(2),
                totalAtendimentos,
                parcelas
        );
    }

    private void validar(
            BigDecimal valorPlano,
            BigDecimal percentualGerencia,
            BigDecimal percentualBarbeiros,
            List<AtendimentoRateio> atendimentos
    ) {
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
        if (atendimentos == null) {
            throw new IllegalArgumentException("A lista de atendimentos e obrigatoria.");
        }
    }

    private Map<String, ResumoBarbeiro> agruparAtendimentos(List<AtendimentoRateio> atendimentos) {
        var agrupados = new TreeMap<String, ResumoBarbeiro>();
        for (var atendimento : atendimentos) {
            if (atendimento == null) {
                throw new IllegalArgumentException("A lista contem um atendimento invalido.");
            }
            agrupados.compute(atendimento.barbeiroId(), (id, atual) -> {
                if (atual == null) {
                    return new ResumoBarbeiro(atendimento.barbeiroNome(), 1);
                }
                return new ResumoBarbeiro(atual.nome(), atual.quantidade() + 1);
            });
        }
        return agrupados;
    }

    private List<ParcelaProvisoria> calcularParcelasProvisorias(
            Map<String, ResumoBarbeiro> porBarbeiro,
            int totalAtendimentos,
            BigDecimal fundoBarbeiros
    ) {
        var parcelas = new ArrayList<ParcelaProvisoria>();
        porBarbeiro.forEach((barbeiroId, resumo) -> {
            var valorExato = fundoBarbeiros
                    .multiply(BigDecimal.valueOf(resumo.quantidade()))
                    .divide(BigDecimal.valueOf(totalAtendimentos), ESCALA_CALCULO, RoundingMode.HALF_EVEN);
            var valorBase = valorExato.setScale(2, RoundingMode.DOWN);
            parcelas.add(new ParcelaProvisoria(
                    barbeiroId,
                    resumo.nome(),
                    resumo.quantidade(),
                    valorExato.subtract(valorBase),
                    valorBase
            ));
        });
        return parcelas;
    }

    private void distribuirCentavosResiduais(
            List<ParcelaProvisoria> parcelas,
            BigDecimal fundoBarbeiros
    ) {
        var totalBase = parcelas.stream()
                .map(ParcelaProvisoria::valorArredondado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var centavosRestantes = fundoBarbeiros
                .subtract(totalBase)
                .movePointRight(2)
                .intValueExact();

        var porMaiorResto = parcelas.stream()
                .sorted(Comparator.comparing(ParcelaProvisoria::resto)
                        .reversed()
                        .thenComparing(ParcelaProvisoria::barbeiroId))
                .toList();

        var acrescimos = new HashMap<String, Integer>();
        for (int indice = 0; indice < centavosRestantes; indice++) {
            var parcela = porMaiorResto.get(indice % porMaiorResto.size());
            acrescimos.merge(parcela.barbeiroId(), 1, Integer::sum);
        }

        for (int indice = 0; indice < parcelas.size(); indice++) {
            var parcela = parcelas.get(indice);
            var acrescimo = UM_CENTAVO.multiply(
                    BigDecimal.valueOf(acrescimos.getOrDefault(parcela.barbeiroId(), 0))
            );
            parcelas.set(indice, parcela.comValor(parcela.valorArredondado().add(acrescimo)));
        }
    }

    private Map<String, BigDecimal> distribuirPercentual(
            Map<String, ResumoBarbeiro> porBarbeiro,
            int totalAtendimentos,
            BigDecimal base
    ) {
        var totalUnidades = base.setScale(ESCALA_PERCENTUAL, RoundingMode.HALF_UP)
                .movePointRight(ESCALA_PERCENTUAL)
                .longValueExact();
        var unidades = new HashMap<String, Long>();
        var restos = new ArrayList<RestoPercentual>();
        long somaBase = 0;

        for (var entry : porBarbeiro.entrySet()) {
            var numerador = Math.multiplyExact(totalUnidades, entry.getValue().quantidade());
            var valorBase = numerador / totalAtendimentos;
            unidades.put(entry.getKey(), valorBase);
            somaBase = Math.addExact(somaBase, valorBase);
            restos.add(new RestoPercentual(entry.getKey(), numerador % totalAtendimentos));
        }

        restos.sort(Comparator.comparingLong(RestoPercentual::resto)
                .reversed()
                .thenComparing(RestoPercentual::barbeiroId));
        var unidadesRestantes = totalUnidades - somaBase;
        for (long indice = 0; indice < unidadesRestantes; indice++) {
            var item = restos.get((int) (indice % restos.size()));
            unidades.merge(item.barbeiroId(), 1L, Long::sum);
        }

        var resultado = new HashMap<String, BigDecimal>();
        unidades.forEach((id, valor) -> resultado.put(id, BigDecimal.valueOf(valor, ESCALA_PERCENTUAL)));
        return resultado;
    }

    private record ResumoBarbeiro(String nome, int quantidade) {
    }

    private record RestoPercentual(String barbeiroId, long resto) {
    }

    private record ParcelaProvisoria(
            String barbeiroId,
            String barbeiroNome,
            int quantidadeAtendimentos,
            BigDecimal resto,
            BigDecimal valorArredondado
    ) {
        private ParcelaProvisoria comValor(BigDecimal valor) {
            return new ParcelaProvisoria(
                    barbeiroId,
                    barbeiroNome,
                    quantidadeAtendimentos,
                    resto,
                    valor
            );
        }
    }
}
