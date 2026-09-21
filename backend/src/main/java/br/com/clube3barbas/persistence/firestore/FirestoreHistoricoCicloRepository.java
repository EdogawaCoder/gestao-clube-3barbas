package br.com.clube3barbas.persistence.firestore;

import br.com.clube3barbas.domain.assinante.HistoricoCiclo;
import br.com.clube3barbas.domain.assinante.HistoricoCicloRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Repository
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirestoreHistoricoCicloRepository implements HistoricoCicloRepository {

    private static final String COLECAO = "subscriberCycleHistory";

    private final Firestore firestore;

    public FirestoreHistoricoCicloRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public List<HistoricoCiclo> listarPorAssinante(String assinanteId) {
        var documentos = FirestoreSuporte.aguardar(
                firestore.collection(COLECAO).whereEqualTo("subscriberId", assinanteId).get()
        ).getDocuments();
        return documentos.stream()
                .map(this::converter)
                .sorted(Comparator.comparing(HistoricoCiclo::inicio))
                .toList();
    }

    @Override
    public HistoricoCiclo registrar(HistoricoCiclo historicoCiclo) {
        var dados = Map.of(
                "subscriberId", historicoCiclo.assinanteId(),
                "cycleStartsAt", historicoCiclo.inicio().toEpochMilli(),
                "recordedAt", historicoCiclo.registradoEm().toEpochMilli()
        );
        FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(historicoCiclo.id()).set(dados));
        return historicoCiclo;
    }

    private HistoricoCiclo converter(DocumentSnapshot documento) {
        return new HistoricoCiclo(
                documento.getId(),
                documento.getString("subscriberId"),
                Instant.ofEpochMilli(documento.getLong("cycleStartsAt")),
                Instant.ofEpochMilli(documento.getLong("recordedAt"))
        );
    }
}
