package com.FluffyTerror.Umagramm_Backend.controller.auth;

import com.FluffyTerror.Umagramm_Backend.requests.authentication.RecoverRequest;
import com.FluffyTerror.Umagramm_Backend.requests.authentication.RefreshRequest;
import com.FluffyTerror.Umagramm_Backend.requests.authentication.RegisterRequest;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.AuthResponse;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.RefreshResponse;
import com.FluffyTerror.Umagramm_Backend.responses.authentication.RegisterResponse;
import com.FluffyTerror.Umagramm_Backend.service.authentication.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/nigga",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Void> hello (@RequestBody String msg){
        System.out.println(msg);
        return Mono.empty();
    }

    @PostMapping(value = "/register",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<RegisterResponse> register(@RequestPart("data") RegisterRequest request, // Данные пользователя (JSON или поля)
                                           @RequestPart("file") FilePart filePart){
        return authService.register(request, filePart);
    };

    @PostMapping("/recover")
    public Mono<AuthResponse> recover(@RequestBody RecoverRequest request){
        return authService.recover(request.username(),request.recoveryCode());
    }

    @PostMapping("/refresh")
    public Mono<RefreshResponse> refresh(@RequestBody RefreshRequest request){
        return authService.refresh(request.refreshToken());
    }
}
