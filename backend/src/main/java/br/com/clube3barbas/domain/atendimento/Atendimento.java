package br.com.clube3barbas.domain.atendimento;

import java.time.Instant;

public record Atendimento(
        String id,
        String assinanteId,
        String barbeiroId,
        String barbeiroNome,
        Instant dataHora
) {
    public Atendimento {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O ID do atendimento e obrigatorio.");
        }
        if (assinanteId == null || assinanteId.isBlank()) {
            throw new IllegalArgumentException("O assinante do atendimento e obrigatorio.");
        }
        if (barbeiroId == null || barbeiroId.isBlank()) {
            throw new IllegalArgumentException("O barbeiro do atendimento e obrigatorio.");
        }
        if (barbeiroNome == null || barbeiroNome.isBlank()) {
            throw new IllegalArgumentException("O nome do barbeiro do atendimento e obrigatorio.");
        }
        if (dataHora == null) {
            throw new IllegalArgumentException("A data do atendimento e obrigatoria.");
        }
        barbeiroNome = barbeiroNome.trim();
    }
}
