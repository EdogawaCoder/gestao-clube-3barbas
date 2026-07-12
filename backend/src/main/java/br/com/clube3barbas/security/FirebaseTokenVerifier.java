package br.com.clube3barbas.security;

public interface FirebaseTokenVerifier {

    UsuarioAutenticado verificar(String idToken);
}

