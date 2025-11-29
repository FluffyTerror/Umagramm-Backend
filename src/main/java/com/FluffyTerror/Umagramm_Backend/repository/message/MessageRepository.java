package com.FluffyTerror.Umagramm_Backend.repository.message;

import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessageRepository extends ReactiveCrudRepository<Message, Long> {

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY messages.createdDate")
    Flux<Message> findMessagesByChatId(Long chatId);

    @Query("UPDATE messages SET message_state = :newState WHERE chat_id = :chatId")
    Mono<Integer> setMessagesToSeenByChat(String newState, Long chatId); // ewState — это строка (enum.name()), потому что в R2DBC нет @Enumerated.
}
