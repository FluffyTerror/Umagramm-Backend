package com.FluffyTerror.Umagramm_Backend.service.user;

import com.FluffyTerror.Umagramm_Backend.requests.UsersRequest;
import com.FluffyTerror.Umagramm_Backend.enums.FriendStatus;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.friends.FriendsRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.mapper.UserMapper;
import com.FluffyTerror.Umagramm_Backend.responses.friend.FriendResponse;
import com.FluffyTerror.Umagramm_Backend.responses.user.UserResponse;
import com.FluffyTerror.Umagramm_Backend.websockets.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final FriendsRepository friendsRepository;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final UserMapper userMapper;

    public Mono<UserResponse> getInfoAboutCurrentUser(@AuthenticationPrincipal User user) {
        return userRepository.getInfoAboutUser(user.getId()).map(userMapper::toUserResponse);
    }
    public Mono<List<UserResponse>> getInfoAboutUsers(String requesterId, UsersRequest request) {
        if (request.getUserIds() == null || request.getUserIds().isEmpty()) {
            return Mono.just(List.of());
        }

        return userRepository.findAllByIds(request.getUserIds())
                .filterWhen(targetUser -> {
                    // 1. Себя видим всегда
                    if (targetUser.getId().equals(requesterId)) {
                        return Mono.just(true);
                    }

                    // 2. Только друзья
                    return friendsRepository
                            .findFriendshipBySenderIdAndReceiverId(
                                    requesterId,
                                    targetUser.getId(),
                                    FriendStatus.Accepted
                            )
                            .hasElement();
                })
                .map(user -> {
                    UserResponse response = userMapper.toUserResponse(user);

                    // 🔥 ВАЖНО: онлайн через WebSocket
                    boolean isOnline = webSocketSessionRegistry.get(user.getId()) != null;
                    response.setOnline(isOnline);

                    return response;
                })
                .collectList();
    }

    public Mono<Void> updateUserOnlineStatus(String userId) {
        return userRepository.updateLastSeen(userId, LocalDateTime.now());
    }
    public Flux<String> getFriendIds(String userId) {
        return friendsRepository.findAllAcceptedFriendsIds(userId,FriendStatus.Accepted);
    }

    public Mono<Void> broadcastStatus(String userId, boolean isOnline) {
        return getFriendIds(userId)
                .flatMap(friendId -> {
                    WebSocketSession session = webSocketSessionRegistry.get(friendId);

                    if (session != null && session.isOpen()) {
                        try {
                            String json = "{\"type\":\"STATUS\",\"userId\":\"" + userId + "\",\"online\":" + isOnline + "}";
                            return session.send(Mono.just(session.textMessage(json)));
                        } catch (Exception e) {
                            return Mono.empty();
                        }
                    }
                    return Mono.empty();
                })
                .then();
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
