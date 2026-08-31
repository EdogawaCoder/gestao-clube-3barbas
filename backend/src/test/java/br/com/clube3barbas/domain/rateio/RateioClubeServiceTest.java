package br.com.clube3barbas.domain.rateio;

import br.com.clube3barbas.domain.assinante.Assinante;
import br.com.clube3barbas.domain.assinante.AssinanteRepository;
import br.com.clube3barbas.domain.atendimento.Atendimento;
import br.com.clube3barbas.domain.atendimento.AtendimentoRepository;
import br.com.clube3barbas.persistence.memoria.MemoriaAssinanteRepository;
import br.com.clube3barbas.persistence.memoria.MemoriaAtendimentoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateioClubeServiceTest {

    private final AssinanteRepository assinantes = new MemoriaAssinanteRepository();
    private final AtendimentoRepository atendimentos = new MemoriaAtendimentoRepository();
    private final RateioClubeService service = new RateioClubeService(assinantes, atendimentos, new RateioService());

    @Test
    void entregaTodoOFundoAoUnicoBarbeiroQueAtendeuOAssinante() {
        var cliente = assinantes.salvar(assinante("cliente-1", "Cliente Um"));
        atendimentos.salvar(atendimento(cliente.id(), "b1", "Barbeiro 1"));

        var resultado = service.calcularParaAssinante(cliente.id());

        assertThat(resultado.parcelas()).singleElement().satisfies(parcela ->
                assertThat(parcela.valor()).isEqualByComparingTo("80.00"));
    }

    @Test
    void divideEntreDoisBarbeirosQuandoCadaUmAtendeUmaVez() {
        var cliente = assinantes.salvar(assinante("cliente-1", "Cliente Um"));
        atendimentos.salvar(atendimento(cliente.id(), "b1", "Barbeiro 1"));
        atendimentos.salvar(atendimento(cliente.id(), "b2", "Barbeiro 2"));

        var resultado = service.calcularParaAssinante(cliente.id());

        assertThat(resultado.parcelas())
                .extracting(ParcelaBarbeiro::valor)
                .containsExactly(new BigDecimal("40.00"), new BigDecimal("40.00"));
    }

    @Test
    void rejeitaAssinanteInexistente() {
        assertThatThrownBy(() -> service.calcularParaAssinante("nao-existe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    void somaORateioDeVariosAssinantesNoFechamentoGeral() {
        var clienteA = assinantes.salvar(assinante("cliente-a", "Cliente A"));
        var clienteB = assinantes.salvar(assinante("cliente-b", "Cliente B"));
        atendimentos.salvar(atendimento(clienteA.id(), "b1", "Barbeiro 1"));
        atendimentos.salvar(atendimento(clienteB.id(), "b1", "Barbeiro 1"));

        var resultado = service.calcularGeral();

        assertThat(resultado.totalAssinantesAtendidos()).isEqualTo(2);
        assertThat(resultado.valorTotalPlanos()).isEqualByComparingTo("400.00");
        assertThat(resultado.valorTotalGerencia()).isEqualByComparingTo("240.00");
        assertThat(resultado.valorTotalBarbeiros()).isEqualByComparingTo("160.00");
        assertThat(resultado.parcelas()).singleElement().satisfies(parcela -> {
            assertThat(parcela.barbeiroId()).isEqualTo("b1");
            assertThat(parcela.quantidadeAtendimentos()).isEqualTo(2);
            assertThat(parcela.valor()).isEqualByComparingTo("160.00");
        });
    }

    @Test
    void ignoraAssinantesSemAtendimentoNoFechamentoGeral() {
        assinantes.salvar(assinante("cliente-sem-visita", "Sem Visita"));

        var resultado = service.calcularGeral();

        assertThat(resultado.totalAssinantesAtendidos()).isZero();
        assertThat(resultado.parcelas()).isEmpty();
    }

    private Assinante assinante(String id, String nome) {
        return new Assinante(
                id, nome,
                new BigDecimal("200.00"), new BigDecimal("60"), new BigDecimal("40"),
                Instant.now()
        );
    }

    private Atendimento atendimento(String assinanteId, String barbeiroId, String barbeiroNome) {
        return new Atendimento(
                java.util.UUID.randomUUID().toString(), assinanteId, barbeiroId, barbeiroNome, Instant.now()
        );
    }
}
