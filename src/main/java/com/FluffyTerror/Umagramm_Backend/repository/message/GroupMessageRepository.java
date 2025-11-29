package com.FluffyTerror.Umagramm_Backend.repository.message;

import com.FluffyTerror.Umagramm_Backend.models.message.GroupMessage;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface GroupMessageRepository extends ReactiveCrudRepository<GroupMessage, Long> {
    @Query("SELECT * FROM group_messages WHERE group_chat_id = :chatId ORDER BY group_messages.createdDate")
    Flux<GroupMessage> findMessagesByChatId(Long chatId);

    @Query("UPDATE group_messages SET message_state = :newState WHERE group_chat_id = :chatId")
    Mono<Integer> setMessagesToSeenByChat(String newState, Long groupChatId); // ewState — это строка (enum.name()), потому что в R2DBC нет @Enumerated.
}
