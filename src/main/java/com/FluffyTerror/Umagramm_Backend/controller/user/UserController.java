package com.FluffyTerror.Umagramm_Backend.controller.user;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.requests.UsersRequest;
import com.FluffyTerror.Umagramm_Backend.responses.friend.FriendResponse;
import com.FluffyTerror.Umagramm_Backend.responses.user.UserResponse;
import com.FluffyTerror.Umagramm_Backend.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User")
public class UserController {
    private final UserService userService;

    @GetMapping
    public Mono<UserResponse> getInfoAboutCurrentUser(@AuthenticationPrincipal User user) {
        return userService.getInfoAboutCurrentUser(user);
    }

    @PostMapping("/fetch")
    public Mono<List<UserResponse>> getInfoAboutUsers(@AuthenticationPrincipal User user,@RequestBody UsersRequest request) {
        return userService.getInfoAboutUsers(user.getId(),request);
    }

    @GetMapping("/friends")
    public Mono<List<FriendResponse>> getAllFriends(Authentication authentication) {
        return userService.getMyFriends(authentication)
                .collectList();
    }
}
