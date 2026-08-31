package br.com.clube3barbas.persistence.firestore;

import br.com.clube3barbas.domain.atendimento.Atendimento;
import br.com.clube3barbas.domain.atendimento.AtendimentoRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirestoreAtendimentoRepository implements AtendimentoRepository {

    private static final String COLECAO = "attendances";

    private final Firestore firestore;

    public FirestoreAtendimentoRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public List<Atendimento> listarPorAssinante(String assinanteId) {
        var documentos = FirestoreSuporte.aguardar(
                firestore.collection(COLECAO).whereEqualTo("subscriberId", assinanteId).get()
        ).getDocuments();
        return documentos.stream()
                .map(this::converter)
                .sorted(Comparator.comparing(Atendimento::dataHora))
                .toList();
    }

    @Override
    public List<Atendimento> listarTodos() {
        var documentos = FirestoreSuporte.aguardar(firestore.collection(COLECAO).get()).getDocuments();
        return documentos.stream()
                .map(this::converter)
                .sorted(Comparator.comparing(Atendimento::dataHora))
                .toList();
    }

    @Override
    public Optional<Atendimento> buscarPorId(String id) {
        var documento = FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(id).get());
        return documento.exists() ? Optional.of(converter(documento)) : Optional.empty();
    }

    @Override
    public Atendimento salvar(Atendimento atendimento) {
        var dados = Map.of(
                "subscriberId", atendimento.assinanteId(),
                "barberId", atendimento.barbeiroId(),
                "barberName", atendimento.barbeiroNome(),
                "occurredAt", atendimento.dataHora().toEpochMilli()
        );
        FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(atendimento.id()).set(dados));
        return atendimento;
    }

    @Override
    public void remover(String id) {
        FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(id).delete());
    }

    private Atendimento converter(DocumentSnapshot documento) {
        return new Atendimento(
                documento.getId(),
                documento.getString("subscriberId"),
                documento.getString("barberId"),
                documento.getString("barberName"),
                Instant.ofEpochMilli(documento.getLong("occurredAt"))
        );
    }
}
