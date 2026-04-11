package com.FluffyTerror.Umagramm_Backend.service.chat;

import com.FluffyTerror.Umagramm_Backend.enums.ChatType;
import com.FluffyTerror.Umagramm_Backend.enums.FriendStatus;
import com.FluffyTerror.Umagramm_Backend.enums.MemberRole;
import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.exceptions.FriendNotFoundException;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.chatMember.ChatMember;
import com.FluffyTerror.Umagramm_Backend.models.friends.Friend;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.chatMember.ChatMemberRepository;
import com.FluffyTerror.Umagramm_Backend.repository.friends.FriendsRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.responses.chat.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final FriendsRepository friendsRepository;
    private final DatabaseClient databaseClient;

    /**
     * Создает новый ПРИВАТНЫЙ чат или возвращает существующий с полной информацией
     */
    @Transactional
    public Mono<ChatResponse> createPrivateChat(String creatorId, String partnerId) {
        return chatMemberRepository.findCommonPrivateChat(creatorId, partnerId)
                .switchIfEmpty(Mono.defer(() -> createNewChat(creatorId, partnerId)))
                .flatMap(chat -> enrichChatWithDetails(chat, creatorId))
                .doOnError(e -> log.error("Error during creating a chat: ", e));
    }

    /**
     * Внутренний метод только для создания записей в БД
     */
    private Mono<Chat> createNewChat(String creatorId, String partnerId) {
        return friendsRepository.findFriendshipBySenderIdAndReceiverId(creatorId,partnerId, FriendStatus.Accepted)
                .switchIfEmpty(Mono.error(new FriendNotFoundException("You are not allowed to create chat with this user, consider adding him into friends first!")))
                .flatMap(friend -> {
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
                });
    }

    /**
     * Маппинг Chat -> ChatResponse
     */
    private Mono<ChatResponse> enrichChatWithDetails(Chat chat, String currentUserId) {
        // Используем один zip, чтобы получить ID партнера и имя чата
        return Mono.zip(
                getPartnerId(chat.getId(), currentUserId).defaultIfEmpty(""),
                getChatName(chat, currentUserId)
        ).flatMap(tuple -> {
            String partnerId = tuple.getT1();
            String chatDisplayName = tuple.getT2();

            // Затем используем второй zip для получения статусов и сообщений
            return Mono.zip(
                    getUnreadMessages(chat.getId(), currentUserId),
                    isPartnerOnline(partnerId, chat.getChatType()),
                    getLastMessage(chat.getId()).defaultIfEmpty(new Message()) // Заглушка, если сообщений нет
            ).map(t -> {
                // t.getT1() -> unread, t.getT2() -> online, t.getT3() -> lastMessage
                return createResponse(
                        chat,
                        currentUserId,
                        partnerId,
                        t.getT1(),
                        t.getT2(),
                        t.getT3(),
                        chatDisplayName
                );
            });
        });
    }

    private ChatResponse createResponse(Chat chat, String senderId, String receiverId, Long unread, Boolean online, Message msg, String chatName) {
        return new ChatResponse(
                chat.getId(),
                chatName,
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

    public Flux<ChatResponse> getUserChats(String userId) {
        // 1. Ищем все записи в chat_member для этого юзера
        return chatMemberRepository.findAllByUserId(userId)
                // 2. Для каждого участника достаем саму сущность Chat
                .flatMap(member -> chatRepository.findById(member.getChatId()))
                // 3. Используем твой готовый метод для сборки ChatResponse
                .flatMap(chat -> enrichChatWithDetails(chat, userId));
    }
}
