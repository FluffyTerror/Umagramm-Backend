package com.FluffyTerror.Umagramm_Backend.service.friends;

import com.FluffyTerror.Umagramm_Backend.enums.FriendStatus;
import com.FluffyTerror.Umagramm_Backend.models.friends.Friend;
import com.FluffyTerror.Umagramm_Backend.repository.friends.FriendsRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.responses.friend.FriendResponse;
import com.FluffyTerror.Umagramm_Backend.websockets.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FriendsService {

    private final FriendsRepository friendsRepository;
    private final UserRepository userRepository;
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    public Mono<Friend> createFriendsRequest(String initiatorId, String receiverId) {
        if (initiatorId.equals(receiverId)) {
            return Mono.error(new IllegalArgumentException("Initiator cannot scan QR-Code of himself"));
        }
        Friend friend = new Friend();
        friend.setSenderId(initiatorId);
        friend.setReceiverId(receiverId);
        friend.setStatus(FriendStatus.Pending);
        friend.setCreatedAt(LocalDateTime.now());
        friend.setUpdatedAt(LocalDateTime.now());
        return friendsRepository.save(friend)
                .flatMap(savedFriend -> {
                    // СРАЗУ ПОСЛЕ СОХРАНЕНИЯ ПЫТАЕМСЯ УВЕДОМИТЬ
                    WebSocketSession session = webSocketSessionRegistry.get(receiverId);
                    if (session != null && session.isOpen()) {
                        String notification = "{\"type\": \"FRIEND_REQUEST\"}";
                        return session.send(Mono.just(session.textMessage(notification)))
                                .thenReturn(savedFriend);
                    }
                    return Mono.just(savedFriend);
                });
    }

    public Mono<Friend> acceptFriendRequest(String senderId, String receiverId) {
        return friendsRepository.findBySenderIdAndReceiverId(senderId, receiverId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No existing friend requests")))
                .flatMap(friendRequest -> {
                    friendRequest.setStatus(FriendStatus.Accepted);
                    friendRequest.setUpdatedAt(LocalDateTime.now());
                    return friendsRepository.save(friendRequest);
                });

    }

    public Mono<Friend> declineFriendRequest(String senderId, String receiverId) {
        return friendsRepository.findBySenderIdAndReceiverId(senderId, receiverId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No existing friend requests")))
                .flatMap(friendRequest -> {
                    friendRequest.setStatus(FriendStatus.Declined);
                    friendRequest.setUpdatedAt(LocalDateTime.now());
                    return friendsRepository.save(friendRequest);
                });
    }

    public Flux<FriendResponse> getAllIncomingRequests(String userId) {
        return friendsRepository.findAllByReceiverIdAndStatus(userId, FriendStatus.Pending)
                .flatMap(friendRequest ->
                        userRepository.findById(friendRequest.getSenderId())
                                .map(senderUser -> new FriendResponse(
                                        // Теперь у нас есть доступ к ОБОИМ объектам:
                                        senderUser.getId(),       // ID того, кто прислал
                                        senderUser.getUsername(), // Имя того, кто прислал
                                        senderUser.getFirstName(),
                                        senderUser.getLastName(),
                                        senderUser.getProfilePhotoId(),
                                        friendRequest.getCreatedAt() // Дата из самой заявки
                                ))
                );
    }
}
