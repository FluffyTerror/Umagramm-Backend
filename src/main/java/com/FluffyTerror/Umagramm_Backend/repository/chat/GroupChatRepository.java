package com.FluffyTerror.Umagramm_Backend.repository.chat;

import com.FluffyTerror.Umagramm_Backend.models.chat.GroupChat;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface GroupChatRepository extends ReactiveCrudRepository<GroupChat, String> {
    @Query("SELECT * FROM group_chat WHERE owner_id = :ownerId")
    Flux<GroupChat> findByOwnerId(String ownerId);

    @Query("UPDATE group_chat SET members_count = :count WHERE id = :chatId")
    Mono<Integer> updateMembersCount(Integer count, Long chatId);

    @Query("UPDATE group_chat SET last_message_id = :messageId, last_message_time = :time WHERE id = :chatId")
    Mono<Integer> updateLastMessage(Long messageId, LocalDateTime time, Long chatId);
}
