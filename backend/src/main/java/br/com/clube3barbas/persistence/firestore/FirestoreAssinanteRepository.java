package br.com.clube3barbas.persistence.firestore;

import br.com.clube3barbas.domain.assinante.Assinante;
import br.com.clube3barbas.domain.assinante.AssinanteRepository;
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
public class FirestoreAssinanteRepository implements AssinanteRepository {

    private static final String COLECAO = "subscribers";

    private final Firestore firestore;

    public FirestoreAssinanteRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public List<Assinante> listar() {
        var documentos = FirestoreSuporte.aguardar(firestore.collection(COLECAO).get()).getDocuments();
        return documentos.stream()
                .map(this::converter)
                .sorted(Comparator.comparing(Assinante::criadoEm))
                .toList();
    }

    @Override
    public Optional<Assinante> buscarPorId(String id) {
        var documento = FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(id).get());
        return documento.exists() ? Optional.of(converter(documento)) : Optional.empty();
    }

    @Override
    public Assinante salvar(Assinante assinante) {
        var dados = Map.of(
                "name", assinante.nome(),
                "planAmountCents", ConversaoMonetaria.paraInteiro(assinante.valorPlano()),
                "managementSharePoints", ConversaoMonetaria.paraInteiro(assinante.percentualGerencia()),
                "barbersSharePoints", ConversaoMonetaria.paraInteiro(assinante.percentualBarbeiros()),
                "createdAt", assinante.criadoEm().toEpochMilli()
        );
        FirestoreSuporte.aguardar(firestore.collection(COLECAO).document(assinante.id()).set(dados));
        return assinante;
    }

    private Assinante converter(DocumentSnapshot documento) {
        return new Assinante(
                documento.getId(),
                documento.getString("name"),
                ConversaoMonetaria.deInteiro(documento.getLong("planAmountCents")),
                ConversaoMonetaria.deInteiro(documento.getLong("managementSharePoints")),
                ConversaoMonetaria.deInteiro(documento.getLong("barbersSharePoints")),
                Instant.ofEpochMilli(documento.getLong("createdAt"))
        );
    }
}
