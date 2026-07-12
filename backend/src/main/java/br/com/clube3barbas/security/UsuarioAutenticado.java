package br.com.clube3barbas.security;

import br.com.clube3barbas.domain.PerfilUsuario;

import java.security.Principal;

public record UsuarioAutenticado(
        String uid,
        String nome,
        String email,
        PerfilUsuario perfil
) implements Principal {

    @Override
    public String getName() {
        return uid;
    }
}

