package com.FluffyTerror.Umagramm_Backend.repository.user;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, String> {

    @Query("SELECT * FROM users WHERE username = :username")
    Mono<User> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);

    @Query("SELECT * from users WHERE id != :publicId")
    Flux<User> getUsersExceptSelf(String publicId);

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
