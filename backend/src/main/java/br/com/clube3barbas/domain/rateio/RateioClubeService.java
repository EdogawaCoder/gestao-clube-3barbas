package br.com.clube3barbas.domain.rateio;

import br.com.clube3barbas.domain.assinante.AssinanteRepository;
import br.com.clube3barbas.domain.atendimento.Atendimento;
import br.com.clube3barbas.domain.atendimento.AtendimentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * Liga o RateioService (a matematica pura do 60/40) aos assinantes e atendimentos
 * persistidos. A regra continua a mesma, calculada por assinante: o fundo dos
 * barbeiros daquele cliente e dividido proporcionalmente aos atendimentos que cada
 * barbeiro fez para ELE. O fechamento geral apenas soma esse resultado entre todos
 * os assinantes ja atendidos.
 */
@Service
public class RateioClubeService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final AssinanteRepository assinanteRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final RateioService rateioService;

    public RateioClubeService(
            AssinanteRepository assinanteRepository,
            AtendimentoRepository atendimentoRepository,
            RateioService rateioService
    ) {
        this.assinanteRepository = assinanteRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.rateioService = rateioService;
    }

    public ResultadoRateio calcularParaAssinante(String assinanteId) {
        var assinante = assinanteRepository.buscarPorId(assinanteId)
                .orElseThrow(() -> new IllegalArgumentException("Assinante nao encontrado."));
        var atendimentos = atendimentoRepository.listarPorAssinante(assinanteId).stream()
                .map(atendimento -> new AtendimentoRateio(atendimento.barbeiroId(), atendimento.barbeiroNome()))
                .toList();

        return rateioService.calcular(
                assinante.valorPlano(),
                assinante.percentualGerencia(),
                assinante.percentualBarbeiros(),
                atendimentos
        );
    }

    public ResultadoGeralRateio calcularGeral() {
        var porAssinante = atendimentoRepository.listarTodos().stream()
                .collect(Collectors.groupingBy(Atendimento::assinanteId, LinkedHashMap::new, Collectors.toList()));

        var valorTotalPlanos = BigDecimal.ZERO.setScale(2);
        var valorTotalGerencia = BigDecimal.ZERO.setScale(2);
        var valorTotalBarbeiros = BigDecimal.ZERO.setScale(2);
        var acumuladoPorBarbeiro = new LinkedHashMap<String, ParcelaAcumulada>();
        var assinantesConsiderados = 0;

        for (var entry : porAssinante.entrySet()) {
            var assinante = assinanteRepository.buscarPorId(entry.getKey()).orElse(null);
            if (assinante == null) {
                // Assinante removido apos os atendimentos terem sido registrados: os
                // atendimentos ficam preservados no historico, mas saem do fechamento.
                continue;
            }
            assinantesConsiderados++;

            var atendimentosRateio = entry.getValue().stream()
                    .map(atendimento -> new AtendimentoRateio(atendimento.barbeiroId(), atendimento.barbeiroNome()))
                    .toList();
            var resultado = rateioService.calcular(
                    assinante.valorPlano(),
                    assinante.percentualGerencia(),
                    assinante.percentualBarbeiros(),
                    atendimentosRateio
            );

            valorTotalPlanos = valorTotalPlanos.add(resultado.valorPlano());
            valorTotalGerencia = valorTotalGerencia.add(resultado.valorGerencia());
            valorTotalBarbeiros = valorTotalBarbeiros.add(resultado.fundoBarbeiros());

            for (var parcela : resultado.parcelas()) {
                acumuladoPorBarbeiro.merge(
                        parcela.barbeiroId(),
                        new ParcelaAcumulada(parcela.barbeiroNome(), parcela.quantidadeAtendimentos(), parcela.valor()),
                        ParcelaAcumulada::somar
                );
            }
        }

        var totalBarbeiros = valorTotalBarbeiros;
        var totalPlanos = valorTotalPlanos;
        var parcelas = acumuladoPorBarbeiro.entrySet().stream()
                .map(entry -> new ParcelaBarbeiro(
                        entry.getKey(),
                        entry.getValue().nome(),
                        entry.getValue().quantidade(),
                        percentual(entry.getValue().valor(), totalBarbeiros),
                        percentual(entry.getValue().valor(), totalPlanos),
                        entry.getValue().valor()
                ))
                .sorted(Comparator.comparing(ParcelaBarbeiro::barbeiroId))
                .toList();

        return new ResultadoGeralRateio(
                valorTotalPlanos,
                valorTotalGerencia,
                valorTotalBarbeiros,
                assinantesConsiderados,
                parcelas
        );
    }

    private BigDecimal percentual(BigDecimal valor, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(4);
        }
        return valor.multiply(CEM).divide(total, 4, RoundingMode.HALF_UP);
    }

    private record ParcelaAcumulada(String nome, int quantidade, BigDecimal valor) {
        private ParcelaAcumulada somar(ParcelaAcumulada outra) {
            return new ParcelaAcumulada(nome, quantidade + outra.quantidade(), valor.add(outra.valor()));
        }
    }
}
