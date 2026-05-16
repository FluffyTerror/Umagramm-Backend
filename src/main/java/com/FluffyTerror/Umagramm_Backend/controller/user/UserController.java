package com.FluffyTerror.Umagramm_Backend.controller.user;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.requests.userInfo.UpdateUserRequest;
import com.FluffyTerror.Umagramm_Backend.requests.groupChat.UsersRequest;
import com.FluffyTerror.Umagramm_Backend.responses.friend.FriendResponse;
import com.FluffyTerror.Umagramm_Backend.responses.user.UserResponse;
import com.FluffyTerror.Umagramm_Backend.service.user.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User")
public class UserController {
    private final UserService userService;
    private final ObjectMapper objectMapper;

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
    @PatchMapping(value = "/change-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<UserResponse> updateUserProfile(
            @RequestPart(value = "data", required = false) String dataStr,
            @RequestPart(value = "file", required = false) FilePart file,
            Authentication authentication) {

        User currentUser = (User) authentication.getPrincipal();

        // Парсим JSON, если он есть
        UpdateUserRequest request = null;
        if (dataStr != null && !dataStr.isBlank()) {
            try {
                request = objectMapper.readValue(dataStr, UpdateUserRequest.class);
            } catch (JsonProcessingException e) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON in 'data' part"));
            }
        }

        return userService.updateUserProfile(currentUser.getId(), request, file);
    }
    @PutMapping("/notifications/toggle")
    public Mono<Void> toggleNotifications(@RequestParam boolean enabled, @AuthenticationPrincipal User user) {
        return userService.toggleNotificationsForUser(enabled,user.getId());
    }
}
