package br.com.clube3barbas.bootstrap;

import br.com.clube3barbas.domain.PerfilUsuario;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;

import java.util.HashMap;
import java.util.Base64;
import java.util.Map;
import java.security.SecureRandom;

public final class ProvisionUserCommand {

    private ProvisionUserCommand() {
    }

    public static void main(String[] args) throws Exception {
        var environment = System.getenv();
        var projectId = required(environment, "GOOGLE_CLOUD_PROJECT");
        var email = required(environment, "CLUBE_USER_EMAIL").trim().toLowerCase(java.util.Locale.ROOT);
        var role = PerfilUsuario.fromClaim(required(environment, "CLUBE_USER_ROLE"));
        var atIndex = email.indexOf('@');
        var defaultName = atIndex > 0 ? email.substring(0, atIndex) : email;
        var displayName = environment.getOrDefault("CLUBE_USER_NAME", defaultName).trim();

        var options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(projectId)
                .build();
        var app = FirebaseApp.initializeApp(options, "clube-user-provisioning");

        try {
            var auth = FirebaseAuth.getInstance(app);
            var user = findOrCreate(auth, email, displayName);
            var claims = new HashMap<String, Object>(user.getCustomClaims());
            claims.put("role", role.name());
            auth.setCustomUserClaims(user.getUid(), claims);
            auth.revokeRefreshTokens(user.getUid());

            System.out.printf("Usuario provisionado: uid=%s, email=%s, perfil=%s%n", user.getUid(), email, role);
            System.out.println("Sessoes anteriores foram revogadas para aplicar o perfil imediatamente.");
            System.out.println("Novo usuario: use 'Esqueci minha senha', confirme o e-mail e cadastre o SMS MFA.");
        } finally {
            app.delete();
        }
    }

    private static UserRecord findOrCreate(
            FirebaseAuth auth,
            String email,
            String displayName
    ) throws FirebaseAuthException {
        try {
            return auth.getUserByEmail(email);
        } catch (FirebaseAuthException exception) {
            if (exception.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                throw exception;
            }
        }

        return auth.createUser(new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(randomUnusablePassword())
                .setDisplayName(displayName)
                .setEmailVerified(false)
                .setDisabled(false));
    }

    private static String randomUnusablePassword() {
        var randomBytes = new byte[48];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static String required(Map<String, String> environment, String name) {
        var value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Variavel obrigatoria ausente: " + name);
        }
        return value;
    }
}
