package com.FluffyTerror.Umagramm_Backend.exceptions;

public class NoRecoveryCodeException extends RuntimeException {
    public NoRecoveryCodeException(String usernameAlreadyExists) {
        super(usernameAlreadyExists);
    }
}
