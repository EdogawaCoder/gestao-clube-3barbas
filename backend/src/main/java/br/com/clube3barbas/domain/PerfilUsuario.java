package br.com.clube3barbas.domain;

import java.text.Normalizer;
import java.util.Locale;

public enum PerfilUsuario {
    GERENTE,
    ADMINISTRATIVO,
    BARBEIRO;

    public static PerfilUsuario fromClaim(Object claim) {
        if (claim == null) {
            throw new IllegalArgumentException("Token sem o perfil de acesso.");
        }

        var normalizado = Normalizer.normalize(claim.toString(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        return switch (normalizado) {
            case "GERENTE", "MANAGER" -> GERENTE;
            case "ADMINISTRATIVO", "ADMINISTRATIVE" -> ADMINISTRATIVO;
            case "BARBEIRO", "BARBER" -> BARBEIRO;
            default -> throw new IllegalArgumentException("Perfil de acesso desconhecido.");
        };
    }
}

