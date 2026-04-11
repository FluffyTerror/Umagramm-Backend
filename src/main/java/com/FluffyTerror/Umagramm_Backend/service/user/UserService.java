package com.FluffyTerror.Umagramm_Backend.service.user;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.mapper.UserMapper;
import com.FluffyTerror.Umagramm_Backend.responses.friend.FriendResponse;
import com.FluffyTerror.Umagramm_Backend.responses.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Mono<List<UserResponse>> getAllUsersExceptSelf(Authentication authentication) {
        return userRepository.getUsersExceptSelf(authentication.getName()).map(userMapper::toUserResponse).collectList();
    }

    public Flux<FriendResponse> getMyFriends(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        String currentUserId = currentUser.getId();

        return userRepository.findAllAcceptedFriends(currentUserId)
                .map(friendUser -> new FriendResponse(friendUser.getId(),
                        friendUser.getUsername(),
                        friendUser.getFirstName(),
                        friendUser.getLastName(),
                        friendUser.getProfilePhotoId(), // Передаем ID фотки
                        friendUser.getCreatedDate()));
    }

}
