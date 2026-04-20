package com.FluffyTerror.Umagramm_Backend.service.RecoveryCode;


import com.FluffyTerror.Umagramm_Backend.exceptions.NoRecoveryCodeException;
import com.FluffyTerror.Umagramm_Backend.exceptions.UserNotFoundException;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.repository.userAuth.UserRecoveryCodeRepository;
import com.FluffyTerror.Umagramm_Backend.requests.recovery.FinishRecoveryCodeRequest;
import com.FluffyTerror.Umagramm_Backend.requests.recovery.RestoreAccountRequest;
import com.FluffyTerror.Umagramm_Backend.responses.recovery.RestoreResponse;
import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtService;
import com.FluffyTerror.Umagramm_Backend.service.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecoveryCodeService {
    private final UserRecoveryCodeRepository userRecoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public Mono<Void> finishRecoveryCode(String userId, FinishRecoveryCodeRequest request) {
        return userRecoveryCodeRepository.updateVaultData(
                        userId,
                        request.encryptedPrivateKey(),
                        request.salt(),
                        request.nonce()
                )
                .flatMap(updatedRows -> {
                    if (updatedRows == 0) {
                        return Mono.error(new RuntimeException(
                                "Сейф не найден. Выполните первый шаг регистрации."
                        ));
                    }
                    return Mono.empty();
                });
    }

    public Mono<RestoreResponse> restoreAccount(RestoreAccountRequest request) {
        return userRepository.findByUsername(request.username()).switchIfEmpty(Mono.error(new UserNotFoundException("User not found"))).flatMap(user -> userRecoveryCodeRepository.findById(user.getId()).flatMap(vault -> {

            if (!passwordEncoder.matches(request.recoveryProof(), vault.getRecoveryCodeHash())) {
                return Mono.error(new BadCredentialsException("Wrong recovery code"));
            }


            return refreshTokenService.create(UUID.fromString(user.getId())).map(rt -> new RestoreResponse(jwtService.generateAccessToken(user), rt.getToken(), vault.getEncryptedPrivateKey(), vault.getSalt(),vault.getNonce()));
        }));
    }
}
