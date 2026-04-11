package com.FluffyTerror.Umagramm_Backend.repository.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param; // Обязательный импорт
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_date DESC")
    Flux<Message> findMessagesByChatId(@Param("chatId") String chatId);

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_date DESC LIMIT 1")
    Mono<Message> findLastMessageByChatId(@Param("chatId") String chatId);

    // 1. Добавлены @Param
    // 2. Заменено status на message_state (согласно Entity)
    @Query("""
        SELECT COUNT(*) FROM messages 
        WHERE chat_id = :chatId 
          AND sender_id != :currentUserId 
          AND status != 'SEEN'
    """)
    Mono<Long> countUnreadMessages(@Param("chatId") String chatId, @Param("currentUserId") String currentUserId);

    @Modifying
    @Query("""
    UPDATE messages 
    SET status = :targetStatus 
    WHERE chat_id = :chatId 
      AND status = :oldStatus 
      AND id <= :lastMessageId
      AND sender_id != :currentUserId
""")
    Mono<Integer> setMessagesToSeenByChat(
            @Param("targetStatus") MessageState targetStatus,
            @Param("oldStatus") MessageState oldStatus,
            @Param("chatId") String chatId,
            @Param("lastMessageId") Long lastMessageId,
            @Param("currentUserId") String currentUserId
    );
}