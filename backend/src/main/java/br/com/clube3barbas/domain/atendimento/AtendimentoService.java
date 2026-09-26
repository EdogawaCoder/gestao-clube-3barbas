package br.com.clube3barbas.domain.atendimento;

import br.com.clube3barbas.domain.assinante.AssinanteRepository;
import br.com.clube3barbas.domain.barbeiro.BarbeiroRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AtendimentoService {

    /** Folga para diferenca de relogio entre o navegador e o servidor. */
    private static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(5);

    private final AtendimentoRepository repository;
    private final AssinanteRepository assinanteRepository;
    private final BarbeiroRepository barbeiroRepository;

    public AtendimentoService(
            AtendimentoRepository repository,
            AssinanteRepository assinanteRepository,
            BarbeiroRepository barbeiroRepository
    ) {
        this.repository = repository;
        this.assinanteRepository = assinanteRepository;
        this.barbeiroRepository = barbeiroRepository;
    }

    public List<Atendimento> listarPorAssinante(String assinanteId) {
        return repository.listarPorAssinante(assinanteId);
    }

    /**
     * Registra um atendimento. dataHora permite lancar visitas passadas (ex.: dentro
     * de um ciclo anterior); se vier nula, vale o instante atual. Datas futuras sao
     * recusadas -- um ciclo agendado so recebe visitas quando chegar.
     */
    public Atendimento registrar(String assinanteId, String barbeiroId, Instant dataHora) {
        assinanteRepository.buscarPorId(assinanteId)
                .orElseThrow(() -> new IllegalArgumentException("Assinante nao encontrado."));
        var barbeiro = barbeiroRepository.buscarPorId(barbeiroId)
                .orElseThrow(() -> new IllegalArgumentException("Barbeiro nao encontrado."));

        var agora = Instant.now();
        var quando = dataHora != null ? dataHora : agora;
        if (quando.isAfter(agora.plus(TOLERANCIA_RELOGIO))) {
            throw new IllegalArgumentException("A data do atendimento nao pode estar no futuro.");
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
