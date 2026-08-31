package br.com.clube3barbas.domain.atendimento;

import br.com.clube3barbas.domain.assinante.AssinanteRepository;
import br.com.clube3barbas.domain.barbeiro.BarbeiroRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AtendimentoService {

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

    public Atendimento registrar(String assinanteId, String barbeiroId) {
        assinanteRepository.buscarPorId(assinanteId)
                .orElseThrow(() -> new IllegalArgumentException("Assinante nao encontrado."));
        var barbeiro = barbeiroRepository.buscarPorId(barbeiroId)
                .orElseThrow(() -> new IllegalArgumentException("Barbeiro nao encontrado."));

        var atendimento = new Atendimento(
                UUID.randomUUID().toString(),
                assinanteId,
                barbeiro.id(),
                barbeiro.nome(),
                Instant.now()
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
