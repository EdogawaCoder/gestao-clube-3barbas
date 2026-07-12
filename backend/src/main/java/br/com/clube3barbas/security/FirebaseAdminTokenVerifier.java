package br.com.clube3barbas.security;

import br.com.clube3barbas.domain.PerfilUsuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.firebase.enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseAdminTokenVerifier implements FirebaseTokenVerifier {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAdminTokenVerifier(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public UsuarioAutenticado verificar(String idToken) {
        try {
            var decoded = firebaseAuth.verifyIdToken(idToken, true);
            if (!decoded.isEmailVerified()) {
                throw new IllegalArgumentException("E-mail ainda nao verificado.");
            }
            var perfil = PerfilUsuario.fromClaim(decoded.getClaims().get("role"));
            var nome = decoded.getName() == null ? decoded.getUid() : decoded.getName();
            return new UsuarioAutenticado(decoded.getUid(), nome, decoded.getEmail(), perfil);
        } catch (FirebaseAuthException | IllegalArgumentException exception) {
            throw new TokenInvalidoException("Token Firebase invalido ou revogado.", exception);
        }
    }
}
