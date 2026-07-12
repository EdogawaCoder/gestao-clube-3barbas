package br.com.clube3barbas.domain.rateio;

public record AtendimentoRateio(String barbeiroId, String barbeiroNome) {

    public AtendimentoRateio {
        if (barbeiroId == null || barbeiroId.isBlank()) {
            throw new IllegalArgumentException("O ID do barbeiro e obrigatorio.");
        }
        if (barbeiroNome == null || barbeiroNome.isBlank()) {
            throw new IllegalArgumentException("O nome do barbeiro e obrigatorio.");
        }
        barbeiroId = barbeiroId.trim();
        barbeiroNome = barbeiroNome.trim();
    }
}

