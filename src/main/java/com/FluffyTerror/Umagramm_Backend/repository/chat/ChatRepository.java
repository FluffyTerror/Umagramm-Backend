package com.FluffyTerror.Umagramm_Backend.repository.chat;

import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatRepository extends ReactiveCrudRepository<Chat, Long> {
    @Query("SELECT * FROM chat WHERE chat.senderId = :senderId OR chat.recipientId = :senderId")
    Flux<Chat> findChatBySenderId(String senderId);// поиск всех чатов юзера

    @Query("SELECT * FROM chat WHERE (chat.senderId = :senderId AND chat.recipientId = :recipientId) OR chat.senderId = :recipientId AND chat.recipientId = :senderId")
    Flux<Chat> findChatBySenderIdAndRecipientId(String senderId,String recipientId); //поиск конкретного чата между юзерами
}
