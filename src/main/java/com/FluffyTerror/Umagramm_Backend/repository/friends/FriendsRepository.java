package com.FluffyTerror.Umagramm_Backend.repository.friends;

import com.FluffyTerror.Umagramm_Backend.enums.FriendStatus;
import com.FluffyTerror.Umagramm_Backend.models.friends.Friend;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FriendsRepository extends ReactiveCrudRepository<Friend, String> {
    Mono<Friend> findBySenderIdAndReceiverId(String SenderId, String receiverId);
    Flux<Friend> findAllByReceiverIdAndStatus(String receiverId, FriendStatus status);
}
