package br.com.clube3barbas.domain.rateio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateioServiceTest {

    private final RateioService service = new RateioService();

    @Test
    void entregaTodoOFundoAoUnicoBarbeiroQueAtendeu() {
        var resultado = service.calcular(
                new BigDecimal("200.00"),
                new BigDecimal("60"),
                new BigDecimal("40"),
                List.of(atendimento("b1", "Barbeiro 1"))
        );

        assertThat(resultado.valorGerencia()).isEqualByComparingTo("120.00");
        assertThat(resultado.fundoBarbeiros()).isEqualByComparingTo("80.00");
        assertThat(resultado.valorNaoAlocado()).isEqualByComparingTo("0.00");
        assertThat(resultado.parcelas()).singleElement().satisfies(parcela -> {
            assertThat(parcela.quantidadeAtendimentos()).isEqualTo(1);
            assertThat(parcela.percentualDoFundo()).isEqualByComparingTo("100.0000");
            assertThat(parcela.percentualDoPlano()).isEqualByComparingTo("40.0000");
            assertThat(parcela.valor()).isEqualByComparingTo("80.00");
        });
    }

    @Test
    void divideIgualmenteQuandoDoisBarbeirosFazemUmAtendimentoCada() {
        var resultado = service.calcular(
                new BigDecimal("200.00"),
                new BigDecimal("60"),
                new BigDecimal("40"),
                List.of(
                        atendimento("b1", "Barbeiro 1"),
                        atendimento("b2", "Barbeiro 2")
                )
        );

        assertThat(resultado.parcelas())
                .extracting(ParcelaBarbeiro::valor)
                .containsExactly(new BigDecimal("40.00"), new BigDecimal("40.00"));
    }

    @Test
    void calculaDoisTercosEUmTercoSemPerderCentavos() {
        var resultado = service.calcular(
                new BigDecimal("200.00"),
                new BigDecimal("60"),
                new BigDecimal("40"),
                List.of(
                        atendimento("b1", "Barbeiro 1"),
                        atendimento("b2", "Barbeiro 2"),
                        atendimento("b1", "Barbeiro 1")
                )
        );

        assertThat(resultado.parcelas()).satisfiesExactly(
                barbeiro1 -> {
                    assertThat(barbeiro1.quantidadeAtendimentos()).isEqualTo(2);
                    assertThat(barbeiro1.percentualDoPlano()).isEqualByComparingTo("26.6667");
                    assertThat(barbeiro1.valor()).isEqualByComparingTo("53.33");
                },
                barbeiro2 -> {
                    assertThat(barbeiro2.quantidadeAtendimentos()).isEqualTo(1);
                    assertThat(barbeiro2.percentualDoPlano()).isEqualByComparingTo("13.3333");
                    assertThat(barbeiro2.valor()).isEqualByComparingTo("26.67");
                }
        );

        var somaBarbeiros = resultado.parcelas().stream()
                .map(ParcelaBarbeiro::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somaBarbeiros).isEqualByComparingTo(resultado.fundoBarbeiros());
    }

    @Test
    void usaDesempateDeterministicoAoDistribuirCentavoResidual() {
        var resultado = service.calcular(
                new BigDecimal("0.01"),
                BigDecimal.ZERO,
                new BigDecimal("100"),
                List.of(
                        atendimento("c", "C"),
                        atendimento("a", "A"),
                        atendimento("b", "B")
                )
        );

        assertThat(resultado.parcelas())
                .extracting(ParcelaBarbeiro::barbeiroId, ParcelaBarbeiro::valor)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("a", new BigDecimal("0.01")),
                        org.assertj.core.groups.Tuple.tuple("b", new BigDecimal("0.00")),
                        org.assertj.core.groups.Tuple.tuple("c", new BigDecimal("0.00"))
                );
    }

    @Test
    void mantemFundoNaoAlocadoQuandoNaoHaAtendimento() {
        var resultado = service.calcular(
                new BigDecimal("200.00"),
                new BigDecimal("60"),
                new BigDecimal("40"),
                List.of()
        );

        assertThat(resultado.parcelas()).isEmpty();
        assertThat(resultado.valorNaoAlocado()).isEqualByComparingTo("80.00");
    }

    @Test
    void agrupaPeloIdMesmoQuandoONomeFoiAtualizado() {
        var resultado = service.calcular(
                new BigDecimal("200.00"),
                new BigDecimal("60"),
                new BigDecimal("40"),
                List.of(
                        atendimento("b1", "Joao"),
                        atendimento("b1", "Joao da Silva")
                )
        );

        assertThat(resultado.parcelas()).singleElement().satisfies(parcela -> {
            assertThat(parcela.barbeiroId()).isEqualTo("b1");
            assertThat(parcela.quantidadeAtendimentos()).isEqualTo(2);
            assertThat(parcela.valor()).isEqualByComparingTo("80.00");
        });
    }

    @Test
    void conservaOTotalMonetarioEmDiversasCombinacoes() {
        for (int centavos = 1; centavos <= 250; centavos++) {
            for (int quantidade = 1; quantidade <= 12; quantidade++) {
                var atendimentos = java.util.stream.IntStream.range(0, quantidade)
                        .mapToObj(indice -> atendimento("b" + (indice % 5), "Barbeiro " + (indice % 5)))
                        .toList();
                var resultado = service.calcular(
                        BigDecimal.valueOf(centavos, 2),
                        new BigDecimal("60"),
                        new BigDecimal("40"),
                        atendimentos
                );
                var somaParcelas = resultado.parcelas().stream()
                        .map(ParcelaBarbeiro::valor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                assertThat(somaParcelas).isEqualByComparingTo(resultado.fundoBarbeiros());
                assertThat(resultado.valorGerencia().add(somaParcelas))
                        .isEqualByComparingTo(resultado.valorPlano());
            }
        }
    }

    @Test
    void conservaOsPercentuaisExibidos() {
        var resultado = service.calcular(
                new BigDecimal("200.00"),
                new BigDecimal("60"),
                new BigDecimal("40"),
                List.of(
                        atendimento("a", "A"),
                        atendimento("b", "B"),
                        atendimento("c", "C")
                )
        );

        var percentualFundo = resultado.parcelas().stream()
                .map(ParcelaBarbeiro::percentualDoFundo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var percentualPlano = resultado.parcelas().stream()
                .map(ParcelaBarbeiro::percentualDoPlano)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(percentualFundo).isEqualByComparingTo("100.0000");
        assertThat(percentualPlano).isEqualByComparingTo("40.0000");
    }

    @Test
    void rejeitaPercentuaisQueNaoSomamCem() {
        assertThatThrownBy(() -> service.calcular(
                new BigDecimal("200.00"),
                new BigDecimal("60"),
                new BigDecimal("30"),
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100%");
    }

    private AtendimentoRateio atendimento(String id, String nome) {
        return new AtendimentoRateio(id, nome);
    }
}
