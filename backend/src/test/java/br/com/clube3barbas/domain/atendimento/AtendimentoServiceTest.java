package br.com.clube3barbas.domain.atendimento;

import br.com.clube3barbas.domain.assinante.Assinante;
import br.com.clube3barbas.domain.assinante.HistoricoCiclo;
import br.com.clube3barbas.domain.barbeiro.Barbeiro;
import br.com.clube3barbas.persistence.memoria.MemoriaAssinanteRepository;
import br.com.clube3barbas.persistence.memoria.MemoriaAtendimentoRepository;
import br.com.clube3barbas.persistence.memoria.MemoriaBarbeiroRepository;
import br.com.clube3barbas.persistence.memoria.MemoriaHistoricoCicloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtendimentoServiceTest {

    private final MemoriaAssinanteRepository assinantes = new MemoriaAssinanteRepository();
    private final MemoriaBarbeiroRepository barbeiros = new MemoriaBarbeiroRepository();
    private final MemoriaHistoricoCicloRepository historico = new MemoriaHistoricoCicloRepository();
    private final AtendimentoService service = new AtendimentoService(
            new MemoriaAtendimentoRepository(), assinantes, barbeiros, historico
    );

    private final Instant agora = Instant.now();
    // Linha do tempo do assinante: encerrado -> vigente (contem hoje) -> agendado.
    private final Instant encerrado = agora.minus(40, ChronoUnit.DAYS);
    private final Instant vigente = agora.minus(10, ChronoUnit.DAYS);
    private final Instant agendado = vigente.plus(Assinante.DURACAO_CICLO_DIAS, ChronoUnit.DAYS);

    @BeforeEach
    void preparar() {
        assinantes.salvar(new Assinante(
                "cliente", "Cliente", new BigDecimal("200"), new BigDecimal("60"), new BigDecimal("40"), agendado, encerrado
        ));
        historico.registrar(new HistoricoCiclo("h1", "cliente", encerrado, agora));
        historico.registrar(new HistoricoCiclo("h2", "cliente", vigente, agora));
        barbeiros.salvar(new Barbeiro("b1", "Pedro", true));
    }

    @Test
    void qualquerPerfilRegistraNoCicloVigenteAteHoje() {
        var semData = service.registrar("cliente", "b1", null, false);
        var ontem = service.registrar("cliente", "b1", agora.minus(1, ChronoUnit.DAYS), false);

        assertThat(semData.dataHora()).isBetween(agora, Instant.now());
        assertThat(ontem.dataHora()).isEqualTo(agora.minus(1, ChronoUnit.DAYS));
    }

    @Test
    void somenteGerenteRegistraEmCicloEncerrado() {
        var dataNoEncerrado = encerrado.plus(3, ChronoUnit.DAYS);

        assertThatThrownBy(() -> service.registrar("cliente", "b1", dataNoEncerrado, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apenas o gerente");
        assertThat(service.registrar("cliente", "b1", dataNoEncerrado, true).dataHora()).isEqualTo(dataNoEncerrado);
    }

    @Test
    void somenteGerenteRegistraEmCicloAgendado() {
        var dataNoAgendado = agendado.plus(2, ChronoUnit.DAYS);

        assertThatThrownBy(() -> service.registrar("cliente", "b1", dataNoAgendado, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Apenas o gerente");
        assertThat(service.registrar("cliente", "b1", dataNoAgendado, true).dataHora()).isEqualTo(dataNoAgendado);
    }

    @Test
    void somenteGerenteRegistraDataFuturaDentroDoVigente() {
        var amanha = agora.plus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> service.registrar("cliente", "b1", amanha, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data futura");
        assertThat(service.registrar("cliente", "b1", amanha, true).dataHora()).isEqualTo(amanha);
    }

    @Test
    void recusaDataForaDeQualquerCicloMesmoParaOGerente() {
        var antesDeTudo = encerrado.minus(1, ChronoUnit.DAYS);

        assertThatThrownBy(() -> service.registrar("cliente", "b1", antesDeTudo, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dentro de um ciclo");
    }
}
