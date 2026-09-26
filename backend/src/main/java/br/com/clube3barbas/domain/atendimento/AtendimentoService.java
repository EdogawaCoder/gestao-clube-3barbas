package br.com.clube3barbas.domain.atendimento;

import br.com.clube3barbas.domain.assinante.Assinante;
import br.com.clube3barbas.domain.assinante.AssinanteRepository;
import br.com.clube3barbas.domain.assinante.HistoricoCiclo;
import br.com.clube3barbas.domain.assinante.HistoricoCicloRepository;
import br.com.clube3barbas.domain.barbeiro.BarbeiroRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import java.util.List;
import java.util.UUID;

@Service
public class AtendimentoService {

    /** Folga para diferenca de relogio entre o navegador e o servidor. */
    private static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(5);

    private final AtendimentoRepository repository;
    private final AssinanteRepository assinanteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final HistoricoCicloRepository historicoCicloRepository;

    public AtendimentoService(
            AtendimentoRepository repository,
            AssinanteRepository assinanteRepository,
            BarbeiroRepository barbeiroRepository,
            HistoricoCicloRepository historicoCicloRepository
    ) {
        this.repository = repository;
        this.assinanteRepository = assinanteRepository;
        this.barbeiroRepository = barbeiroRepository;
        this.historicoCicloRepository = historicoCicloRepository;
    }

    public List<Atendimento> listarPorAssinante(String assinanteId) {
        return repository.listarPorAssinante(assinanteId);
    }

    /**
     * Registra um atendimento; sem dataHora, vale o instante atual. A data precisa
     * cair dentro de algum ciclo do assinante. No ciclo vigente (o que contem hoje)
     * qualquer perfil registra, ate o momento atual. Em ciclos encerrados ou
     * agendados -- e em datas futuras -- so o gerente (podeForaDoVigente) registra.
     */
    public Atendimento registrar(String assinanteId, String barbeiroId, Instant dataHora, boolean podeForaDoVigente) {
        var assinante = assinanteRepository.buscarPorId(assinanteId)
                .orElseThrow(() -> new IllegalArgumentException("Assinante nao encontrado."));
        var barbeiro = barbeiroRepository.buscarPorId(barbeiroId)
                .orElseThrow(() -> new IllegalArgumentException("Barbeiro nao encontrado."));

        var agora = Instant.now();
        var quando = dataHora != null ? dataHora : agora;
        var ciclos = Stream.concat(
                Stream.of(assinante.cicloInicio()),
                historicoCicloRepository.listarPorAssinante(assinanteId).stream().map(HistoricoCiclo::inicio)
        ).toList();
        if (ciclos.stream().noneMatch(inicio -> dentroDoCiclo(quando, inicio))) {
            throw new IllegalArgumentException("A data do atendimento precisa estar dentro de um ciclo do assinante.");
        }
        if (!podeForaDoVigente) {
            var noVigente = ciclos.stream().anyMatch(inicio -> dentroDoCiclo(quando, inicio) && dentroDoCiclo(agora, inicio));
            if (!noVigente) {
                throw new IllegalArgumentException("Apenas o gerente pode registrar atendimentos em ciclos encerrados ou agendados.");
            }
            if (quando.isAfter(agora.plus(TOLERANCIA_RELOGIO))) {
                throw new IllegalArgumentException("Apenas o gerente pode registrar atendimentos com data futura.");
            }
        }

        var atendimento = new Atendimento(
                UUID.randomUUID().toString(),
                assinanteId,
                barbeiro.id(),
                barbeiro.nome(),
                quando
        );
        return repository.salvar(atendimento);
    }

    private static boolean dentroDoCiclo(Instant instante, Instant cicloInicio) {
        return !instante.isBefore(cicloInicio)
                && instante.isBefore(cicloInicio.plus(Assinante.DURACAO_CICLO_DIAS, ChronoUnit.DAYS));
    }

    /**
     * Remove um atendimento lancado por engano. So apaga se ele realmente pertence
     * ao assinante informado na URL, para evitar que alguem exclua o atendimento de
     * outro cliente so adivinhando o ID.
     */
    public void remover(String assinanteId, String atendimentoId) {
        var atendimento = repository.buscarPorId(atendimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Atendimento nao encontrado."));
        if (!atendimento.assinanteId().equals(assinanteId)) {
            throw new IllegalArgumentException("O atendimento nao pertence a este assinante.");
        }
        repository.remover(atendimentoId);
    }
}
