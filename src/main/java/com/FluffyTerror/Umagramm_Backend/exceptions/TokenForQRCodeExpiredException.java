package com.FluffyTerror.Umagramm_Backend.exceptions;

public class TokenForQRCodeExpiredException extends RuntimeException {
    public TokenForQRCodeExpiredException(String usernameAlreadyExists) {
        super(usernameAlreadyExists);
    }
}
