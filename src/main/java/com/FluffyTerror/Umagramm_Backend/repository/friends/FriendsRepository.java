package com.FluffyTerror.Umagramm_Backend.repository.friends;

import com.FluffyTerror.Umagramm_Backend.enums.FriendStatus;
import com.FluffyTerror.Umagramm_Backend.models.friends.Friend;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FriendsRepository extends ReactiveCrudRepository<Friend, String> {
    Mono<Friend> findBySenderIdAndReceiverId(String senderId, String receiverId);

    Flux<Friend> findAllByReceiverIdAndStatus(String receiverId, FriendStatus status);

    @Query("""
            SELECT * FROM public.friends fr
            WHERE (
                (fr.sender_id = :senderId AND fr.receiver_id = :receiverId)
                OR
                (fr.sender_id = :receiverId AND fr.receiver_id = :senderId)
            )
            AND fr.status = :status
            LIMIT 1
            """)
    Mono<Friend> findFriendshipBySenderIdAndReceiverId(String senderId, String receiverId, FriendStatus status);

    @Query("""
    SELECT CASE 
            WHEN fr.sender_id = :userId THEN fr.receiver_id
            ELSE fr.sender_id
        END AS friend_id
    FROM friends fr
    WHERE (fr.sender_id = :userId OR fr.receiver_id = :userId)
      AND fr.status IN ('Accepted', 'Pending')
""")
    Flux<String> findAllAcceptedFriendsIds(String userId);

}
