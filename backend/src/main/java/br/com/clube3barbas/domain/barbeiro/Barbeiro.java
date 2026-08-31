package br.com.clube3barbas.domain.barbeiro;

public record Barbeiro(String id, String nome, boolean ativo) {

    public Barbeiro {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O ID do barbeiro e obrigatorio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do barbeiro e obrigatorio.");
        }
        nome = nome.trim();
    }
}
