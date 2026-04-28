package com.FluffyTerror.Umagramm_Backend.controller.firebase;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
@RestController
public class FirebaseController {
    private final UserService userService;

    public FirebaseController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/api/v1/users/fcm-token")
    public Mono<ResponseEntity<String>> updateFcmToken(@AuthenticationPrincipal User user,
                                                       @RequestBody Map<String, String> payload) {
        String token = payload.get("token");

        // Возвращаем Mono, чтобы Spring WebFlux сам на него подписался
        return userService.updateFcmToken(user.getId(), token)
                .thenReturn(ResponseEntity.ok("Token updated successfully"))
                .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body("Error: " + e.getMessage())));
    }
}
