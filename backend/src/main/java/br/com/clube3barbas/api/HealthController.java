package br.com.clube3barbas.api;

import com.google.cloud.firestore.Firestore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final ObjectProvider<Firestore> firestoreProvider;

    public HealthController(ObjectProvider<Firestore> firestoreProvider) {
        this.firestoreProvider = firestoreProvider;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "clube-3-barbas-api",
                "timestamp", Instant.now().toString()
        );
    }

    /**
     * Diagnostico de conectividade com o Firestore, usado para validar a credencial
     * de service account fora do Cloud Run (por exemplo, no Render). Nao expoe dados:
     * apenas confirma se o Admin SDK consegue alcancar o banco do projeto configurado.
     */
    @GetMapping("/health/firestore")
    public Map<String, Object> firestoreHealth() {
        var resultado = new LinkedHashMap<String, Object>();
        resultado.put("timestamp", Instant.now().toString());

        var firestore = firestoreProvider.getIfAvailable();
        if (firestore == null) {
            resultado.put("firestore", "DISABLED");
            return resultado;
        }

        try {
            firestore.collection("healthcheck").listDocuments().iterator().hasNext();
            resultado.put("firestore", "UP");
        } catch (Exception exception) {
            resultado.put("firestore", "DOWN");
            resultado.put("message", exception.getMessage());
        }
        return resultado;
    }
}

