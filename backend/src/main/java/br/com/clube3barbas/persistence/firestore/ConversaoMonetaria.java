package br.com.clube3barbas.persistence.firestore;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Converte valores monetarios e percentuais (BigDecimal, na borda Java) para a
 * representacao inteira do Firestore (centavos/pontos-base), conforme
 * docs/modelo-firestore.md. As duas conversoes usam a mesma formula (x100),
 * entao um unico par de metodos serve para dinheiro e para percentual.
 */
final class ConversaoMonetaria {

    private ConversaoMonetaria() {
    }

    static long paraInteiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    static BigDecimal deInteiro(long valor) {
        return BigDecimal.valueOf(valor, 2);
    }
}
