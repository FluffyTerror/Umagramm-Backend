package com.FluffyTerror.Umagramm_Backend.repository.user;

import com.FluffyTerror.Umagramm_Backend.models.user.GroupUser;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface GroupUserRepository extends ReactiveCrudRepository<GroupUser, Long> {
    @Query("SELECT * FROM group_user WHERE group_chat_id = :chatId")
    Flux<GroupUser> findMembers(Long chatId);

    @Query("SELECT * FROM group_user WHERE group_chat_id = :chatId AND user_id = :userId")
    Mono<GroupUser> findMember(Long chatId, String userId);

    @Query("DELETE FROM group_user WHERE group_chat_id = :chatId AND user_id = :userId")
    Mono<Integer> removeMember(Long chatId, String userId);

    @Query("SELECT COUNT(*) FROM group_user WHERE group_chat_id = :chatId")
    Mono<Integer> countMembers(Long chatId);
}
