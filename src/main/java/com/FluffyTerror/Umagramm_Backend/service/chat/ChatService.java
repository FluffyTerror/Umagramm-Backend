package com.FluffyTerror.Umagramm_Backend.service.chat;

import com.FluffyTerror.Umagramm_Backend.enums.ChatType;
import com.FluffyTerror.Umagramm_Backend.enums.MemberRole;
import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.chatMember.ChatMember;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.chatMember.ChatMemberRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.responses.chat.ChatResponse;
import com.FluffyTerror.Umagramm_Backend.mapper.ChatMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    /**
     * Создает новый ПРИВАТНЫЙ чат или возвращает существующий с полной информацией
     */
    @Transactional
    public Mono<ChatResponse> createPrivateChat(String creatorId, String partnerId) {
        return chatMemberRepository.findCommonPrivateChat(creatorId, partnerId)
                .switchIfEmpty(Mono.defer(() -> createNewChat(creatorId, partnerId)))
                .flatMap(chat -> enrichChatWithDetails(chat, creatorId))
                .doOnError(e -> log.error("Ошибка при создании/получении чата: ", e));
    }

    /**
     * Внутренний метод только для создания записей в БД
     */
    private Mono<Chat> createNewChat(String creatorId, String partnerId) {
        Chat chat = new Chat();
        chat.setId(UUID.randomUUID().toString());
        chat.setChatType(ChatType.PRIVATE);
        chat.markNew();

        return chatRepository.save(chat)
                .flatMap(savedChat -> {
                    ChatMember m1 = createMember(savedChat.getId(), creatorId);
                    ChatMember m2 = createMember(savedChat.getId(), partnerId);
                    return chatMemberRepository.save(m1)
                            .then(chatMemberRepository.save(m2))
                            .thenReturn(savedChat);
                });
    }

    /**
     * Маппинг Chat -> ChatResponse без использования сторонних мапперов
     */
    private Mono<ChatResponse> enrichChatWithDetails(Chat chat, String currentUserId) {
        return getPartnerId(chat.getId(), currentUserId)
                .defaultIfEmpty("")
                .flatMap(partnerId ->
                        // Сначала получаем данные, которые всегда есть
                        Mono.zip(
                                getUnreadMessages(chat.getId(), currentUserId),
                                isPartnerOnline(partnerId, chat.getChatType())
                        ).flatMap(tuple ->
                                // Затем подмешиваем сообщение, которое может отсутствовать
                                getLastMessage(chat.getId())
                                        .map(msg -> createResponse(chat, currentUserId, partnerId, tuple.getT1(), tuple.getT2(), msg))
                                        .defaultIfEmpty(createResponse(chat, currentUserId, partnerId, tuple.getT1(), tuple.getT2(), null))
                        )
                );
    }

    private ChatResponse createResponse(Chat chat, String senderId, String receiverId, Long unread, Boolean online, Message msg) {
        return new ChatResponse(
                chat.getId(),
                senderId,
                receiverId,
                chat.getChatType().name(),
                unread,
                msg != null ? msg.getContent() : null,
                msg != null ? msg.getCreatedDate() : null,
                online
        );
    }

    // --- Вспомогательные методы остаются почти такими же ---

    private Mono<String> getPartnerId(String chatId, String currentUserId) {
        return chatMemberRepository.findAllByChatId(chatId)
                .filter(member -> !member.getUserId().equals(currentUserId))
                .next()
                .map(ChatMember::getUserId);
    }

    public Mono<Long> getUnreadMessages(String chatId, String currentUserId) {
        return messageRepository.countUnreadMessages(chatId, currentUserId, MessageState.SENT)
                .defaultIfEmpty(0L);
    }

    public Mono<Message> getLastMessage(String chatId) {
        return messageRepository.findLastMessageByChatId(chatId);
    }

    public Mono<Boolean> isPartnerOnline(String partnerId, ChatType type) {
        if (type == ChatType.GROUP || partnerId.isEmpty()) return Mono.just(false);
        return userRepository.findById(partnerId)
                .map(User::isOnline)
                .defaultIfEmpty(false);
    }

    private ChatMember createMember(String chatId, String userId) {
        ChatMember member = new ChatMember();
        member.setChatId(chatId);
        member.setUserId(userId);
        member.setRole(MemberRole.MEMBER);
        member.setJoinedAt(LocalDateTime.now());
        member.setNew(true);
        return member;
    }
    /**
     * Определяет название чата для конкретного пользователя.
     * Для групп — это заголовок группы.
     * Для личных чатов — это имя и фамилия собеседника.
     */
    public Mono<String> getChatName(Chat chat, String currentUserId) {
        if (chat.getChatType() == ChatType.GROUP) {
            return Mono.just(chat.getTitle() != null ? chat.getTitle() : "Group Chat");
        }

        // Для личного чата ищем имя "другого" человека
        return getPartnerId(chat.getId(), currentUserId)
                .flatMap(userRepository::findById)
                .map(u -> {
                    String firstName = u.getFirstName() != null ? u.getFirstName() : "";
                    String lastName = u.getLastName() != null ? u.getLastName() : "";
                    String full = (firstName + " " + lastName).trim();
                    return full.isEmpty() ? "Unknown User" : full;
                })
                .defaultIfEmpty("Unknown User");
    }
}
