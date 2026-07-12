package br.com.clube3barbas.domain.assinante;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StatusAssinantePolicyTest {

    private final StatusAssinantePolicy policy = new StatusAssinantePolicy();
    private final Instant agora = Instant.parse("2026-07-11T12:00:00Z");

    @Test
    void ativoQuandoAindaEstaDentroDaVigencia() {
        assertThat(policy.calcular(
                SituacaoCadastral.ACTIVE,
                agora.plusSeconds(1),
                agora
        )).isEqualTo(StatusAssinante.ACTIVE);
    }

    @Test
    void pendenteNoInstanteExatoDoFimDaVigencia() {
        assertThat(policy.calcular(
                SituacaoCadastral.ACTIVE,
                agora,
                agora
        )).isEqualTo(StatusAssinante.PENDING);
    }

    @Test
    void cancelamentoEExclusaoPrevalecemSobrePagamento() {
        var pagoAte = agora.plusSeconds(86_400);

        assertThat(policy.calcular(SituacaoCadastral.CANCELED, pagoAte, agora))
                .isEqualTo(StatusAssinante.CANCELED);
        assertThat(policy.calcular(SituacaoCadastral.DELETED, pagoAte, agora))
                .isEqualTo(StatusAssinante.DELETED);
    }
}
