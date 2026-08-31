package br.com.clube3barbas.persistence.firestore;

import com.google.api.core.ApiFuture;

import java.util.concurrent.ExecutionException;

/**
 * O cliente Java do Firestore retorna ApiFuture (async). Como o resto da API e
 * bloqueante (Spring MVC classico), os repositorios aguardam o resultado aqui,
 * num unico lugar, em vez de repetir o mesmo try/catch em cada um.
 */
final class FirestoreSuporte {

    private FirestoreSuporte() {
    }

    static <T> T aguardar(ApiFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operacao no Firestore interrompida.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Falha ao acessar o Firestore.", exception.getCause());
        }
    }
}
