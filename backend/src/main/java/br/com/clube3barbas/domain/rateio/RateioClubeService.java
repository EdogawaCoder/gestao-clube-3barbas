package br.com.clube3barbas.domain.rateio;

import br.com.clube3barbas.domain.assinante.Assinante;
import br.com.clube3barbas.domain.assinante.AssinanteRepository;
import br.com.clube3barbas.domain.assinante.HistoricoCicloRepository;
import br.com.clube3barbas.domain.atendimento.Atendimento;
import br.com.clube3barbas.domain.atendimento.AtendimentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Liga o RateioService (a matematica pura do 60/40) aos assinantes e atendimentos
 * persistidos. A regra continua a mesma, calculada por assinante: o fundo dos
 * barbeiros daquele cliente e dividido proporcionalmente aos atendimentos que cada
 * barbeiro fez para ELE. Os fechamentos agregados apenas somam esse resultado entre
 * varios assinantes, usando janelas de tempo diferentes conforme o caso de uso:
 * o ciclo vigente de cada um (calcularGeral, usado na operacao do dia a dia) ou um
 * periodo livre escolhido pelo Gerente (calcularPorPeriodo, para fechamento/relatorio).
 */
@Service
public class RateioClubeService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final AssinanteRepository assinanteRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final HistoricoCicloRepository historicoCicloRepository;
    private final RateioService rateioService;

    public RateioClubeService(
            AssinanteRepository assinanteRepository,
            AtendimentoRepository atendimentoRepository,
            HistoricoCicloRepository historicoCicloRepository,
            RateioService rateioService
    ) {
        this.assinanteRepository = assinanteRepository;
        this.atendimentoRepository = atendimentoRepository;
        this.historicoCicloRepository = historicoCicloRepository;
        this.rateioService = rateioService;
    }

    public ResultadoRateio calcularParaAssinante(String assinanteId) {
        return calcularParaAssinante(assinanteId, null);
    }

    /**
     * Rateio de um ciclo especifico do assinante (o atual, um anterior do historico
     * ou um agendado). Sem cicloInicio, usa o ciclo atual (Assinante.cicloInicio).
     * So conta as visitas daquela janela de 30 dias.
     */
    public ResultadoRateio calcularParaAssinante(String assinanteId, Instant cicloInicio) {
        var assinante = assinanteRepository.buscarPorId(assinanteId)
                .orElseThrow(() -> new IllegalArgumentException("Assinante nao encontrado."));
        var inicio = cicloInicio == null ? assinante.cicloInicio() : cicloDoAssinante(assinante, cicloInicio);
        var fim = inicio.plus(Assinante.DURACAO_CICLO_DIAS, ChronoUnit.DAYS);
        var atendimentos = atendimentoRepository.listarPorAssinante(assinanteId).stream()
                .filter(atendimento -> !atendimento.dataHora().isBefore(inicio) && atendimento.dataHora().isBefore(fim))
                .map(atendimento -> new AtendimentoRateio(atendimento.barbeiroId(), atendimento.barbeiroNome()))
                .toList();

        return rateioService.calcular(
                assinante.valorPlano(),
                assinante.percentualGerencia(),
                assinante.percentualBarbeiros(),
                atendimentos
        );
    }

    private Instant cicloDoAssinante(Assinante assinante, Instant cicloInicio) {
        var existe = assinante.cicloInicio().equals(cicloInicio)
                || historicoCicloRepository.listarPorAssinante(assinante.id()).stream()
                        .anyMatch(ciclo -> ciclo.inicio().equals(cicloInicio));
        if (!existe) {
            throw new IllegalArgumentException("Ciclo nao encontrado para este assinante.");
        }
        return cicloInicio;
    }

    /** Soma o ciclo vigente de cada assinante -- "quanto devo agora", por assinante. */
    public ResultadoGeralRateio calcularGeral() {
        return agregar(agruparTodosPorAssinante(), (assinante, atendimentos) -> atendimentos.stream()
                .filter(atendimento -> assinante.dentroDoCicloVigente(atendimento.dataHora()))
                .toList());
    }

    /**
     * Soma um periodo livre, igual para todos os assinantes -- "quanto vou pagar no
     * fechamento de tal mes", independente do ciclo individual de cada um. Os
     * assinantes considerados sao os que tiveram ao menos um atendimento dentro do
     * periodo; os demais nao entram (nao pagaram/nao foram atendidos nessa janela).
     */
    public ResultadoGeralRateio calcularPorPeriodo(Instant inicio, Instant fimExclusivo) {
        if (inicio == null || fimExclusivo == null) {
            throw new IllegalArgumentException("O inicio e o fim do periodo sao obrigatorios.");
        }
        if (!inicio.isBefore(fimExclusivo)) {
            throw new IllegalArgumentException("O inicio do periodo deve ser anterior ao fim.");
        }
        return agregar(agruparTodosPorAssinante(), (assinante, atendimentos) -> atendimentos.stream()
                .filter(atendimento -> !atendimento.dataHora().isBefore(inicio)
                        && atendimento.dataHora().isBefore(fimExclusivo))
                .toList());
    }

    private Map<String, List<Atendimento>> agruparTodosPorAssinante() {
        return atendimentoRepository.listarTodos().stream()
                .collect(Collectors.groupingBy(Atendimento::assinanteId, LinkedHashMap::new, Collectors.toList()));
    }

    /**
     * Agrega o rateio de varios assinantes. O "filtro" decide, para cada assinante,
     * quais dos atendimentos brutos dele entram no calculo -- e' o unico ponto que
     * muda entre calcularGeral (ciclo individual) e calcularPorPeriodo (janela unica).
     */
    private ResultadoGeralRateio agregar(
            Map<String, List<Atendimento>> atendimentosPorAssinante,
            BiFunction<Assinante, List<Atendimento>, List<Atendimento>> filtro
    ) {
        var valorTotalPlanos = BigDecimal.ZERO.setScale(2);
        var valorTotalGerencia = BigDecimal.ZERO.setScale(2);
        var valorTotalBarbeiros = BigDecimal.ZERO.setScale(2);
        var acumuladoPorBarbeiro = new LinkedHashMap<String, ParcelaAcumulada>();
        var assinantesConsiderados = 0;

        for (var entry : atendimentosPorAssinante.entrySet()) {
            var assinante = assinanteRepository.buscarPorId(entry.getKey()).orElse(null);
            if (assinante == null) {
                // Assinante removido apos os atendimentos terem sido registrados: os
                // atendimentos ficam preservados no historico, mas saem do fechamento.
                continue;
            }

            var atendimentosRateio = filtro.apply(assinante, entry.getValue()).stream()
                    .map(atendimento -> new AtendimentoRateio(atendimento.barbeiroId(), atendimento.barbeiroNome()))
                    .toList();
            if (atendimentosRateio.isEmpty()) {
                continue;
            }
            assinantesConsiderados++;

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
