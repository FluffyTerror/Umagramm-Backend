package com.FluffyTerror.Umagramm_Backend.repository.chat;

import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatRepository extends ReactiveCrudRepository<Chat, String> {
    @Query("SELECT * FROM chat WHERE chat.senderId = :senderId OR chat.recipientId = :senderId")
    Flux<Chat> findAllChatsBySenderId(@Param("senderId") String senderId);// поиск всех чатов юзера

    @Query("SELECT * FROM chat WHERE (chat.senderId = :senderId AND chat.recipientId = :recipientId) OR chat.senderId = :recipientId AND chat.recipientId = :senderId")
    Mono<Chat> findChatBySenderIdAndRecipientId(@Param("senderId") String senderId, @Param("recipientId") String recipientId); //поиск конкретного чата между юзерами
}
