package com.FluffyTerror.Umagramm_Backend.service.authentication;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.exceptions.UserAlreadyExistsException;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.models.userAuth.UserRecoveryCode;
import com.FluffyTerror.Umagramm_Backend.repository.UserAuthRepository;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.requests.authentication.RegisterRequest;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.AuthResponse;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.RefreshResponse;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.RegisterResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtService;
import com.FluffyTerror.Umagramm_Backend.service.jwt.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final UserAuthRepository authRepo;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    // private final RecoveryCodeService recoveryCodeService; //

    public Mono<RegisterResponse> register(RegisterRequest request, FilePart photo) {
        return users.existsByUsername(request.username())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new UserAlreadyExistsException("Username already exists"));
                    }

                    // 1. Подготавливаем ID пользователя заранее
                    String userId = UUID.randomUUID().toString();

                    // 2. Если фото передано, запускаем цепочку сохранения файла
                    Mono<Long> photoIdMono = (photo != null)
                            ? attachmentService.saveFile(photo, userId)
                            .flatMap(filePath -> {
                                Attachment attachment = new Attachment();
                                attachment.setUrl(filePath);
                                attachment.setType(MessageType.IMAGE);
                                // messageId остается null, так как это аватар
                                return attachmentRepository.save(attachment);
                            })
                            .map(Attachment::getId)
                            : Mono.justOrEmpty(java.util.Optional.empty());

                    // 3. Собираем всё вместе
                    return photoIdMono.defaultIfEmpty(0L) // Заглушка, если фото нет
                            .flatMap(photoId -> {
                                User user = new User();
                                user.setId(userId);
                                user.setUsername(request.username());
                                user.setFirstName(request.firstName());
                                user.setLastName(request.lastName());
                                if (photoId != 0L) {
                                    user.setProfilePhotoId(photoId);
                                }

                                UserRecoveryCode auth = new UserRecoveryCode();
                                auth.setUserId(user.getId());

                                String placeholderCode = "TEMP_CODE";

                                return users.save(user)
                                        .then(authRepo.save(auth))
                                        .then(refreshTokenService.create(UUID.fromString(user.getId())))
                                        .map(rt -> new RegisterResponse(
                                                rt.getToken(),
                                                jwtService.generateAccessToken(user),
                                                placeholderCode
                                        ));
                            });
                });
    }

    public Mono<AuthResponse> recover(String username, String recoveryCode) {
        return users.findByUsername(username)
                .flatMap(user ->
                        authRepo.findById(user.getId())
                                /* .filter(auth ->
                                        recoveryCodeService.matches(
                                                recoveryCode,
                                                auth.getRecoveryCodeHash()
                                        )
                                ) */ //
                                .switchIfEmpty(Mono.error(new RuntimeException("Invalid credentials")))
                                .flatMap(auth ->
                                        refreshTokenService.create(UUID.fromString(user.getId()))
                                                .map(rt -> new AuthResponse(
                                                        rt.getToken(),
                                                        jwtService.generateAccessToken(user),
                                                        user.getUsername()
                                                ))
                                )
                );
    }

    public Mono<RefreshResponse> refresh(String refreshToken) {
        return refreshTokenService.validate(refreshToken)
                .flatMap(refreshTokenService::rotate)
                .flatMap(rt ->
                        users.findById(String.valueOf(rt.getUserId()))
                                .map(user -> new RefreshResponse(
                                        jwtService.generateAccessToken(user),
                                        LocalDateTime.from(Instant.now().plus(30,ChronoUnit.MINUTES))
                                ))
                );
    }
}

