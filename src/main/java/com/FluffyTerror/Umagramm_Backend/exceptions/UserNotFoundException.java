package com.FluffyTerror.Umagramm_Backend.exceptions;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String usernameAlreadyExists) {
        super(usernameAlreadyExists);
    }
}
