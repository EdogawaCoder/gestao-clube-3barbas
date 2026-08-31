package br.com.clube3barbas.persistence.memoria;

import br.com.clube3barbas.domain.atendimento.Atendimento;
import br.com.clube3barbas.domain.atendimento.AtendimentoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "false")
public class MemoriaAtendimentoRepository implements AtendimentoRepository {

    private final Map<String, Atendimento> atendimentos = new ConcurrentHashMap<>();

    @Override
    public List<Atendimento> listarPorAssinante(String assinanteId) {
        return atendimentos.values().stream()
                .filter(atendimento -> atendimento.assinanteId().equals(assinanteId))
                .sorted(Comparator.comparing(Atendimento::dataHora))
                .toList();
    }

    @Override
    public List<Atendimento> listarTodos() {
        return atendimentos.values().stream()
                .sorted(Comparator.comparing(Atendimento::dataHora))
                .toList();
    }

    @Override
    public Optional<Atendimento> buscarPorId(String id) {
        return Optional.ofNullable(atendimentos.get(id));
    }

    @Override
    public Atendimento salvar(Atendimento atendimento) {
        atendimentos.put(atendimento.id(), atendimento);
        return atendimento;
    }

    @Override
    public void remover(String id) {
        atendimentos.remove(id);
    }
}
