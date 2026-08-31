package br.com.clube3barbas.persistence.memoria;

import br.com.clube3barbas.domain.assinante.Assinante;
import br.com.clube3barbas.domain.assinante.AssinanteRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "false")
public class MemoriaAssinanteRepository implements AssinanteRepository {

    private final Map<String, Assinante> assinantes = new ConcurrentHashMap<>();

    @Override
    public List<Assinante> listar() {
        return assinantes.values().stream()
                .sorted(Comparator.comparing(Assinante::criadoEm))
                .toList();
    }

    @Override
    public Optional<Assinante> buscarPorId(String id) {
        return Optional.ofNullable(assinantes.get(id));
    }

    @Override
    public Assinante salvar(Assinante assinante) {
        assinantes.put(assinante.id(), assinante);
        return assinante;
    }
}
