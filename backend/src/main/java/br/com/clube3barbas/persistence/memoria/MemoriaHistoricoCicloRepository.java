package br.com.clube3barbas.persistence.memoria;

import br.com.clube3barbas.domain.assinante.HistoricoCiclo;
import br.com.clube3barbas.domain.assinante.HistoricoCicloRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "false")
public class MemoriaHistoricoCicloRepository implements HistoricoCicloRepository {

    private final Map<String, HistoricoCiclo> ciclos = new ConcurrentHashMap<>();

    @Override
    public List<HistoricoCiclo> listarPorAssinante(String assinanteId) {
        return ciclos.values().stream()
                .filter(ciclo -> ciclo.assinanteId().equals(assinanteId))
                .sorted(Comparator.comparing(HistoricoCiclo::inicio))
                .toList();
    }

    @Override
    public HistoricoCiclo registrar(HistoricoCiclo historicoCiclo) {
        ciclos.put(historicoCiclo.id(), historicoCiclo);
        return historicoCiclo;
    }
}
