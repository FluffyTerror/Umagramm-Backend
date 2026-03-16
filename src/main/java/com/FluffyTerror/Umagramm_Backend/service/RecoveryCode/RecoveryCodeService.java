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
        return userRecoveryCodeRepository.findById(userId).switchIfEmpty(Mono.error(new NoRecoveryCodeException("Код восстановления для пользователя не найден. Завершите первый шаг регистрации."))).flatMap(recoveryCode -> {
            // Кладем в сейф зашифрованный ключ и соль
            recoveryCode.setEncryptedPrivateKey(request.encryptedPrivateKey());
            recoveryCode.setSalt(request.salt());
            return userRecoveryCodeRepository.save(recoveryCode);
        }).then();
    }

    public Mono<RestoreResponse> restoreAccount(RestoreAccountRequest request) {
        return userRepository.findByUsername(request.username()).switchIfEmpty(Mono.error(new UserNotFoundException("Пользователь не найден"))).flatMap(user -> userRecoveryCodeRepository.findById(user.getId()).flatMap(vault -> {
            // 1. Проверяем, подходит ли Пропуск к Печати в базе
            if (!passwordEncoder.matches(request.recoveryProof(), vault.getRecoveryCodeHash())) {
                return Mono.error(new BadCredentialsException("Неверный код восстановления"));
            }

            // 2. Если всё верно, генерируем новые токены (как при логине)
            return refreshTokenService.create(UUID.fromString(user.getId())).map(rt -> new RestoreResponse(jwtService.generateAccessToken(user), rt.getToken(), vault.getEncryptedPrivateKey(), vault.getSalt()));
        }));
    }
}
