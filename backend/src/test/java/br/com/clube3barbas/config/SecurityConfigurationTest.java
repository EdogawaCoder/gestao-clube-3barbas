package br.com.clube3barbas.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigurationTest {

    private final ApplicationSecurityProperties enabled = new ApplicationSecurityProperties(true);

    @Test
    void permiteCabecalhosDeDesenvolvimentoSomenteNoProfileLocal() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThat(SecurityConfiguration.isLocalDevelopment(enabled, environment)).isTrue();
    }

    @Test
    void recusaForaDoProfileLocal() {
        var environment = new MockEnvironment();

        assertThat(SecurityConfiguration.isLocalDevelopment(enabled, environment)).isFalse();
    }

    @Test
    void recusaNoCloudRunMesmoSeOProfileLocalForAtivadoPorEngano() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        environment.setProperty("K_SERVICE", "clube-3-barbas-api");

        assertThat(SecurityConfiguration.isLocalDevelopment(enabled, environment)).isFalse();
    }
}

