package br.com.clube3barbas.domain.assinante;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class StatusAssinantePolicy {

    public StatusAssinante calcular(
            SituacaoCadastral situacaoCadastral,
            Instant pagoAte,
            Instant agora
    ) {
        if (situacaoCadastral == null || agora == null) {
            throw new IllegalArgumentException("Situacao cadastral e instante de referencia sao obrigatorios.");
        }

        return switch (situacaoCadastral) {
            case DELETED -> StatusAssinante.DELETED;
            case CANCELED -> StatusAssinante.CANCELED;
            case ACTIVE -> pagoAte != null && agora.isBefore(pagoAte)
                    ? StatusAssinante.ACTIVE
                    : StatusAssinante.PENDING;
        };
    }
}
