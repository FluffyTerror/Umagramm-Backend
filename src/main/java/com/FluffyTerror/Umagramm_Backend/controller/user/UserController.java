package com.FluffyTerror.Umagramm_Backend.controller.user;

import com.FluffyTerror.Umagramm_Backend.service.user.UserResponse;
import com.FluffyTerror.Umagramm_Backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public Mono<ResponseEntity<List<UserResponse>>> getAllUsers(Authentication authentication) {
        return userService.getAllUsersExceptSelf(authentication).map(
                ResponseEntity::ok);
    }
}
