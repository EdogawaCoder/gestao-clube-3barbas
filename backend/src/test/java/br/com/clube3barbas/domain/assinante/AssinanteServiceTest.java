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
    void reiniciaOCicloAtualArquivandoOAnterior() {
        var cadastrado = service.cadastrar("Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));
        // Ciclo no passado: cadastro e reinicio podem cair no mesmo tick de Instant.now().
        var cicloAnterior = cadastrado.cicloInicio().minus(10, ChronoUnit.DAYS);
        var assinante = repository.salvar(new Assinante(
                cadastrado.id(), cadastrado.nome(), cadastrado.valorPlano(), cadastrado.percentualGerencia(),
                cadastrado.percentualBarbeiros(), cicloAnterior, cadastrado.criadoEm()
        ));

        var reiniciado = service.reiniciarCiclo(assinante.id());

        assertThat(reiniciado.cicloInicio()).isAfter(cicloAnterior);
        assertThat(service.listarCiclosAnteriores(assinante.id()))
                .singleElement()
                .satisfies(ciclo -> assertThat(ciclo.inicio()).isEqualTo(cicloAnterior));
    }

    @Test
    void removeUmCicloEncerradoDaLinhaDoTempo() {
        var assinante = service.cadastrar("Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));
        var cicloUm = assinante.cicloInicio();
        var cicloDois = cicloUm.plus(30, ChronoUnit.DAYS);
        service.atualizar(assinante.id(), "Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"), cicloDois);
        service.atualizar(assinante.id(), "Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"), cicloDois.plus(30, ChronoUnit.DAYS));
        var primeiro = service.listarCiclosAnteriores(assinante.id()).get(0);

        service.removerCicloAnterior(assinante.id(), primeiro.id());

        assertThat(service.listarCiclosAnteriores(assinante.id()))
                .extracting(HistoricoCiclo::inicio)
                .containsExactly(cicloDois);
    }

    @Test
    void rejeitaRemoverCicloDeOutroAssinante() {
        var dono = service.cadastrar("Dono", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));
        var outro = service.cadastrar("Outro", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"));
        service.reiniciarCiclo(dono.id());
        var ciclo = service.listarCiclosAnteriores(dono.id()).get(0);

        assertThatThrownBy(() -> service.removerCicloAnterior(outro.id(), ciclo.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ciclo nao encontrado");
        assertThat(service.listarCiclosAnteriores(dono.id())).hasSize(1);
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
