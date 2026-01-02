package com.FluffyTerror.Umagramm_Backend.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String usernameAlreadyExists) {
        super(usernameAlreadyExists);
    }
}
