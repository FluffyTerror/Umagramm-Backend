package com.FluffyTerror.Umagramm_Backend.repository.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY messages.createdDate")
    Flux<Message> findMessagesByChatId(String chatId);

    @Query("UPDATE messages SET message_state = :newState WHERE chat_id = :chatId")
    Mono<Integer> setMessagesToSeenByChat(String newState, String chatId); // newState — это строка (enum.name()), потому что в R2DBC нет @Enumerated.

    // ДОБАВЛЕНИЕ: Метод для подсчета непрочитанных сообщений
    // Ищем сообщения для чата (Long chatId), адресованные пользователю (String recipientId),
    // с определенным статусом (String stateName).
    @Query("SELECT * FROM messages WHERE chat_id = :chatId AND sender_id != :currentUserId AND status = :stateName")
    Flux<Message> findAllUnreadMessages(String chatId, String currentUserId, MessageState stateName);

    // ДОБАВЛЕНИЕ: Метод для получения последнего сообщения
    // Используем LIMIT 1, чтобы получить только одно сообщение
    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_date DESC LIMIT 1")
    Mono<Message> findLastMessageByChatId(String chatId);
}
