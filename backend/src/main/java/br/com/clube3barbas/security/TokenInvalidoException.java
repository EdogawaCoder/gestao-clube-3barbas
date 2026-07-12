package br.com.clube3barbas.security;

public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}

