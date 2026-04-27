package com.FluffyTerror.Umagramm_Backend.repository.chatMember;

import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.chatMember.ChatMember;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatMemberRepository extends ReactiveCrudRepository<ChatMember, Long> {
    Flux<ChatMember> findAllByUserId(String userId);
    Flux<ChatMember> findAllByChatId(String chatId);
    Mono<ChatMember> findByChatIdAndUserId(String chatId, String userId);
    // Пример запроса для поиска общего приватного чата
    @Query("""
        SELECT c.* FROM chats c 
        JOIN chat_members cm1 ON c.id = cm1.chat_id 
        JOIN chat_members cm2 ON c.id = cm2.chat_id 
        WHERE c.type = 'PRIVATE' 
        AND cm1.user_id = :user1 
        AND cm2.user_id = :user2
        LIMIT 1
    """)
    Mono<Chat> findCommonPrivateChat(String user1, String user2);
}
