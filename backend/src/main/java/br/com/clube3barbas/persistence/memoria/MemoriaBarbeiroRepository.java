package br.com.clube3barbas.persistence.memoria;

import br.com.clube3barbas.domain.barbeiro.Barbeiro;
import br.com.clube3barbas.domain.barbeiro.BarbeiroRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacao sem persistencia real, usada em desenvolvimento local
 * (app.firebase.enabled=false) e nos testes: os dados vivem so na memoria do
 * processo e somem ao reiniciar.
 */
@Repository
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "false")
public class MemoriaBarbeiroRepository implements BarbeiroRepository {

    private final Map<String, Barbeiro> barbeiros = new ConcurrentHashMap<>();

    @Override
    public List<Barbeiro> listarAtivos() {
        return barbeiros.values().stream()
                .filter(Barbeiro::ativo)
                .sorted(Comparator.comparing(Barbeiro::nome))
                .toList();
    }

    @Override
    public Optional<Barbeiro> buscarPorId(String id) {
        return Optional.ofNullable(barbeiros.get(id));
    }

    @Override
    public Barbeiro salvar(Barbeiro barbeiro) {
        barbeiros.put(barbeiro.id(), barbeiro);
        return barbeiro;
    }
}
