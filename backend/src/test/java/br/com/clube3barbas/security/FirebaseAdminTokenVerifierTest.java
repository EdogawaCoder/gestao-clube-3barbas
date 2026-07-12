package br.com.clube3barbas.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FirebaseAdminTokenVerifierTest {

    @Test
    void reconheceSessaoAutenticadaComSegundoFator() {
        var claims = Map.<String, Object>of(
                "firebase",
                Map.of("sign_in_provider", "password", "sign_in_second_factor", "phone")
        );

        assertThat(FirebaseAdminTokenVerifier.possuiSegundoFator(claims)).isTrue();
    }

    @Test
    void rejeitaSessaoSemSegundoFator() {
        var claims = Map.<String, Object>of(
                "firebase",
                Map.of("sign_in_provider", "password")
        );

        assertThat(FirebaseAdminTokenVerifier.possuiSegundoFator(claims)).isFalse();
        assertThat(FirebaseAdminTokenVerifier.possuiSegundoFator(Map.of())).isFalse();
    }
}

