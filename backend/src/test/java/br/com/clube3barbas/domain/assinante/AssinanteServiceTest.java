package br.com.clube3barbas.domain.assinante;

import br.com.clube3barbas.persistence.memoria.MemoriaAssinanteRepository;
import br.com.clube3barbas.persistence.memoria.MemoriaHistoricoCicloRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssinanteServiceTest {

    private final AssinanteRepository repository = new MemoriaAssinanteRepository();
    private final HistoricoCicloRepository historicoCicloRepository = new MemoriaHistoricoCicloRepository();
    private final AssinanteService service = new AssinanteService(repository, historicoCicloRepository);

    @Test
    void cadastroComecaSemHistoricoDeCiclo() {
        var assinante = service.cadastrar("Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));

        assertThat(service.listarCiclosAnteriores(assinante.id())).isEmpty();
    }

    @Test
    void arquivaOCicloAnteriorQuandoOInicioMuda() {
        var assinante = service.cadastrar("Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));
        var novoInicio = assinante.cicloInicio().plus(30, ChronoUnit.DAYS);

        var atualizado = service.atualizar(
                assinante.id(), "Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"), novoInicio
        );

        assertThat(atualizado.cicloInicio()).isEqualTo(novoInicio);
        assertThat(service.listarCiclosAnteriores(assinante.id()))
                .singleElement()
                .satisfies(ciclo -> assertThat(ciclo.inicio()).isEqualTo(assinante.cicloInicio()));
    }

    @Test
    void naoArquivaNadaQuandoOInicioDoCicloNaoMuda() {
        var assinante = service.cadastrar("Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));

        service.atualizar(
                assinante.id(), "Cliente Renomeado", new BigDecimal("250"), new BigDecimal("60"), new BigDecimal("40"),
                assinante.cicloInicio()
        );

        assertThat(service.listarCiclosAnteriores(assinante.id())).isEmpty();
    }

    @Test
    void acumulaVariosCiclosAoLongoDeVariasRenovacoes() {
        var assinante = service.cadastrar("Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));
        var cicloUm = assinante.cicloInicio();
        var cicloDois = cicloUm.plus(30, ChronoUnit.DAYS);
        var cicloTres = cicloDois.plus(30, ChronoUnit.DAYS);

        service.atualizar(assinante.id(), "Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"), cicloDois);
        service.atualizar(assinante.id(), "Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"), cicloTres);

        assertThat(service.listarCiclosAnteriores(assinante.id()))
                .extracting(HistoricoCiclo::inicio)
                .containsExactly(cicloUm, cicloDois);
    }

    @Test
    void excluiOAssinanteDefinitivamente() {
        var assinante = service.cadastrar("Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));

        service.excluir(assinante.id());

        assertThatThrownBy(() -> service.buscarPorId(assinante.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    void rejeitaExcluirAssinanteInexistente() {
        assertThatThrownBy(() -> service.excluir("nao-existe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrado");
    }
}
