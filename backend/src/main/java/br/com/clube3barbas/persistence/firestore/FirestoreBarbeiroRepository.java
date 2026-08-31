package br.com.clube3barbas.persistence.firestore;

import br.com.clube3barbas.domain.barbeiro.Barbeiro;
import br.com.clube3barbas.domain.barbeiro.BarbeiroRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirestoreBarbeiroRepository implements BarbeiroRepository {

    private static final String COLECAO = "barbers";

    private final Firestore firestore;

    public FirestoreBarbeiroRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public List<Barbeiro> listarAtivos() {
        var documentos = FirestoreSuporte.aguardar(
                firestore.collection(COLECAO).whereEqualTo("active", true).get()
        ).getDocuments();
        return documentos.stream()
                .map(this::converter)
                .sorted(Comparator.comparing(Barbeiro::nome))
                .toList();
    }

    @Override
    public Optional<Barbeiro> buscarPorId(String id) {
        var documento = FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(id).get());
        return documento.exists() ? Optional.of(converter(documento)) : Optional.empty();
    }

    @Override
    public Barbeiro salvar(Barbeiro barbeiro) {
        var dados = Map.of(
                "name", barbeiro.nome(),
                "active", barbeiro.ativo()
        );
        FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(barbeiro.id()).set(dados));
        return barbeiro;
    }

    private Barbeiro converter(DocumentSnapshot documento) {
        return new Barbeiro(
                documento.getId(),
                documento.getString("name"),
                Boolean.TRUE.equals(documento.getBoolean("active"))
        );
    }
}
