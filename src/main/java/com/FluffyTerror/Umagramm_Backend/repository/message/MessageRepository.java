package com.FluffyTerror.Umagramm_Backend.repository.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_date DESC")
    Flux<Message> findMessagesByChatId(String chatId);

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_date DESC LIMIT 1")
    Mono<Message> findLastMessageByChatId(String chatId);

    // Подсчет непрочитанных: статус SENT и отправитель не я
    @Query("SELECT COUNT(*) FROM messages WHERE chat_id = :chatId AND sender_id != :userId AND status = :status")
    Mono<Long> countUnreadMessages(String chatId, String userId, MessageState status);

    @Modifying
    @Query("UPDATE messages SET status = :targetStatus WHERE chat_id = :chatId AND status = :oldStatus AND sender_id != :currentUserId")
    Mono<Void> setMessagesToSeenByChat(MessageState targetStatus, MessageState oldStatus, String chatId, String currentUserId);
}
