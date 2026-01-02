package com.FluffyTerror.Umagramm_Backend.service.RecoveryCode;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RecoveryCodeService {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public String generate() {
        return "RC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    public String hash(String code) {
        return encoder.encode(code);
    }

    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
