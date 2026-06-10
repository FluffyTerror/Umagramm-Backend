package com.FluffyTerror.Umagramm_Backend.service.authentication;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.exceptions.UserAlreadyExistsException;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.models.userAuth.UserRecoveryCode;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.repository.userAuth.UserRecoveryCodeRepository;
import com.FluffyTerror.Umagramm_Backend.requests.authentication.RegisterRequest;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.RefreshResponse;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.RegisterResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import com.FluffyTerror.Umagramm_Backend.service.jwt.JwtService;
import com.FluffyTerror.Umagramm_Backend.service.jwt.RefreshTokenService;
import io.minio.MinioClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRecoveryCodeRepository userRecoveryCodeRepository;
    public Mono<@NonNull RegisterResponse> register(RegisterRequest request, FilePart photo) {
        return users.existsByUsername(request.username()).flatMap(exists -> {
            if (exists) {
                return Mono.error(new UserAlreadyExistsException("Username already exists"));
            }

            String userId = UUID.randomUUID().toString();

            //  Определяем ID фотографии: либо сохраняем новую, либо берем константу
            Mono<Long> photoIdMono = (photo != null)
                    ? attachmentService.saveFile(photo, userId)
                    .flatMap(filePath -> {
                        Attachment attachment = new Attachment();
                        attachment.setUrl(filePath);
                        attachment.setType(MessageType.IMAGE);
                        return attachmentRepository.save(attachment);
                    })
                    .map(Attachment::getId)
                    // Если фото не прислали, просто возвращаем ID 1 (наш эталон из БД)
                    : Mono.just(1L);

            //  Основная цепочка регистрации
            return photoIdMono.flatMap(photoId -> {
                User user = new User();
                user.setId(userId);
                user.setUsername(request.username());
                user.setFirstName(request.firstName());
                user.setLastName(request.lastName());

                // Теперь photoId есть всегда (либо новый, либо 1L)
                user.setProfilePhotoId(photoId);

                String rawRecoveryCode = generateRecoveryCode();
                String recoveryCodeHash = passwordEncoder.encode(rawRecoveryCode);

                UserRecoveryCode userRecoveryCode = new UserRecoveryCode();
                userRecoveryCode.setUserId(user.getId());
                userRecoveryCode.setRecoveryCodeHash(recoveryCodeHash);

                return users.save(user)
                        .doOnSuccess(User::markNotNew)
                        .then(userRecoveryCodeRepository.save(userRecoveryCode)
                                .doOnSuccess(saved -> saved.setNew(false)))
                        .then(refreshTokenService.create(UUID.fromString(user.getId())))
                        .map(rt -> new RegisterResponse(
                                rt.getToken(),
                                jwtService.generateAccessToken(user),
                                rawRecoveryCode
                        ));
            });
        });
    }

    public Mono<RefreshResponse> refresh(String refreshToken) {
        return refreshTokenService.validate(refreshToken)
                .flatMap(refreshTokenService::rotate)
                .flatMap(rt ->
                        users.findById(String.valueOf(rt.getUserId()))
                                .map(user ->
                                        new RefreshResponse(
                                                jwtService.generateAccessToken(user),
                                                rt.getToken(),
                                                LocalDateTime.ofInstant(Instant.now().plus(30, ChronoUnit.MINUTES),
                                                        ZoneId.systemDefault()))));
    }

    // Метод для генерации красивого и надежного кода (например: XXXX-XXXX-XXXX-XXXX)
    private String generateRecoveryCode() {
        String chars = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (i > 0 && i % 4 == 0) sb.append("-");
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private Mono<Long> createDefaultAvatar() {
        Attachment attachment = new Attachment();
        // Пишем только имя файла, как это делает attachmentService.saveFile
        attachment.setUrl("default-avatar.jpg");
        attachment.setType(MessageType.IMAGE);

        return attachmentRepository.save(attachment)
                .map(Attachment::getId);
    }
}

