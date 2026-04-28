package com.FluffyTerror.Umagramm_Backend.repository.user;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, String> {

    @Modifying
    @Query("UPDATE users SET fcm_token = :token WHERE id = :userId")
    Mono<Void> updateFcmToken(String userId, String token);

    @Query("SELECT * FROM users WHERE username = :username")
    Mono<User> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);

    @Query("SELECT * from users WHERE id = :publicId")
    Mono<User> getInfoAboutUser(String publicId);

    @Query("""
                UPDATE users
                SET last_seen = :lastSeen
                WHERE id = :userId
            """)
    Mono<Void> updateLastSeen(String userId, LocalDateTime lastSeen);

    @Query("SELECT * from users WHERE id IN (:userIds)")
    Flux<User> findAllByIds(List<String> userIds);

    @Query("SELECT * from users WHERE id = :userId")
    Mono<User> getUserById(String userId);

    @Query("""
                SELECT u.* FROM users u
                JOIN friends f ON (u.id = f.sender_id OR u.id = f.receiver_id)
                WHERE (f.sender_id = :currentUserId OR f.receiver_id = :currentUserId)
                  AND u.id != :currentUserId
                  AND f.status = 'Accepted'
            """)
    Flux<User> findAllAcceptedFriends(String currentUserId);

}
