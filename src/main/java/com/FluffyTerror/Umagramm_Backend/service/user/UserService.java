package com.FluffyTerror.Umagramm_Backend.service.user;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;
import com.FluffyTerror.Umagramm_Backend.requests.userInfo.UpdateUserRequest;
import com.FluffyTerror.Umagramm_Backend.requests.groupChat.UsersRequest;
import com.FluffyTerror.Umagramm_Backend.enums.FriendStatus;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.friends.FriendsRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.mapper.UserMapper;
import com.FluffyTerror.Umagramm_Backend.responses.friend.FriendResponse;
import com.FluffyTerror.Umagramm_Backend.responses.user.UserResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import com.FluffyTerror.Umagramm_Backend.websockets.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
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
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
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

                    // 2. Только друзья и заявки
                    return friendsRepository.findBySenderIdAndReceiverId(requesterId, targetUser.getId())
                            .switchIfEmpty(friendsRepository.findBySenderIdAndReceiverId(targetUser.getId(), requesterId))
                            .filter(f -> f.getStatus() == FriendStatus.Accepted || f.getStatus() == FriendStatus.Pending)
                            .hasElement();
                })
                .map(user -> {
                    UserResponse response = userMapper.toUserResponse(user);


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
        return friendsRepository.findAllAcceptedFriendsIds(userId);
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
    public Mono<UserResponse> updateUserProfile(String userId, UpdateUserRequest request, FilePart file) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new Exception("User not found")))
                .flatMap(user -> {
                    user.markNotNew();

                    // 1. Применяем текстовые изменения (если они есть)
                    if (request != null) {
                        if (request.firstName() != null) user.setFirstName(request.firstName());
                        if (request.lastName() != null) user.setLastName(request.lastName());
                        if (request.username() != null) user.setUsername(request.username());
                    }

                    // 2. Если файла нет, просто переходим к сохранению
                    if (file == null) {
                        return Mono.just(user);
                    }

                    // 3. Если файл есть, сохраняем его и привязываем ID к юзеру
                    return attachmentService.saveFile(file, userId)
                            .flatMap(filePath -> {
                                Attachment attachment = new Attachment();
                                attachment.setType(MessageType.IMAGE);
                                attachment.setUrl(filePath);
                                attachment.setFilename(file.filename());
                                // Можешь добавить вытягивание contentType, если нужно
                                return attachmentRepository.save(attachment);
                            })
                            .map(savedAttachment -> {
                                user.setProfilePhotoId(savedAttachment.getId());
                                return user;
                            });
                })
                .flatMap(userRepository::save)
                .map(userMapper::toUserResponse); // Возвращаем обновленного юзера
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

    public Mono<Void> updateFcmToken(String userId, String token) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setFcmToken(token);
                    user.markNotNew(); // КРИТИЧЕСКИ ВАЖНО: устанавливает isNew = false
                    return userRepository.save(user);
                })
                .then();
    }

    public Mono<Void> toggleNotificationsForUser(boolean enabled, String userId){
        return userRepository.findById(userId)
                .flatMap(u -> {
                    u.markNotNew();
                    u.setNotificationsEnabled(enabled);
                    return userRepository.save(u);
                })
                .then();
    }
}
