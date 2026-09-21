package br.com.clube3barbas.domain.assinante;

import java.time.Instant;

/**
 * Registro de um ciclo que ja foi vigente para o assinante e foi substituido por
 * outro (renovacao ou ajuste manual do inicio do ciclo). O ciclo ATUAL fica em
 * Assinante.cicloInicio; aqui ficam apenas os que ja foram encerrados/trocados,
 * formando a linha do tempo de ciclos do assinante.
 */
public record HistoricoCiclo(
        String id,
        String assinanteId,
        Instant inicio,
        Instant registradoEm
) {
    public HistoricoCiclo {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O ID do historico de ciclo e obrigatorio.");
        }
        if (assinanteId == null || assinanteId.isBlank()) {
            throw new IllegalArgumentException("O assinante do historico de ciclo e obrigatorio.");
        }
        if (inicio == null) {
            throw new IllegalArgumentException("O inicio do ciclo e obrigatorio.");
        }
        if (registradoEm == null) {
            throw new IllegalArgumentException("A data de registro e obrigatoria.");
        }
    }
}
