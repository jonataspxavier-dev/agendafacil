package com.agendafacil.backend.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Já existe uma conta com o e-mail " + email);
    }
}
