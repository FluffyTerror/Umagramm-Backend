package com.FluffyTerror.Umagramm_Backend.service.chat;

import com.FluffyTerror.Umagramm_Backend.enums.ChatType;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Используем для чистоты кода вместо @AllArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository; // Новый репозиторий
    private final ChatMapper chatMapper;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    /**
     * Получает список всех чатов пользователя (и личных, и групповых)
     */
    public Flux<ChatResponse> getUserChats(String currentUserId) {
        // Находим все записи об участии пользователя в чатах
        return chatMemberRepository.findAllByUserId(currentUserId)
                .flatMap(member -> chatRepository.findById(member.getChatId()))
                .flatMap(chat -> enrichChatWithDetails(chat, currentUserId));
    }

    /**
     * Обогащает базовый объект Chat данными для отображения (имя, последнее сообщение и т.д.)
     */
    private Mono<ChatResponse> enrichChatWithDetails(Chat chat, String currentUserId) {
        // Для личного чата нам нужен ID собеседника для статуса "Online"
        Mono<String> partnerIdMono = (chat.getChatType() == ChatType.PRIVATE)
                ? getPartnerId(chat.getId(), currentUserId)
                : Mono.just(""); // Для групп ID собеседника не применим

        return partnerIdMono.flatMap(partnerId ->
                Mono.zip(
                        getChatName(chat, currentUserId),                // Название (Имя друга или Название группы)
                        getUnreadMessages(chat.getId(), currentUserId),   // Непрочитанные
                        getLastMessage(chat.getId()),                     // Последнее сообщение
                        isPartnerOnline(partnerId, chat.getChatType())        // Статус "В сети"
                ).map(tuple -> chatMapper.toChatResponse(
                        chat,
                        tuple.getT1(), // name
                        tuple.getT2(), // unread
                        tuple.getT3(), // lastMsg
                        tuple.getT4()  // online
                ))
        );
    }

    /**
     * Определяет название чата в зависимости от его типа
     */
    public Mono<String> getChatName(Chat chat, String currentUserId) {
        if (chat.getChatType() == ChatType.GROUP) {
            return Mono.just(chat.getTitle() != null ? chat.getTitle() : "Group Chat");
        }

        // Для личного чата ищем имя "другого" человека
        return getPartnerId(chat.getId(), currentUserId)
                .flatMap(userRepository::findById)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .defaultIfEmpty("Unknown User");
    }

    /**
     * Находит ID собеседника в приватном чате
     */
    private Mono<String> getPartnerId(String chatId, String currentUserId) {
        return chatMemberRepository.findAllByChatId(chatId)
                .filter(member -> !member.getUserId().equals(currentUserId))
                .next()
                .map(ChatMember::getUserId);
    }

    /**
     * Создает новый ПРИВАТНЫЙ чат между двумя пользователями
     */
    @Transactional
    public Mono<String> createPrivateChat(String creatorId, String partnerId) {
        // 1. Проверяем, существует ли уже такой приватный чат
        return chatMemberRepository.findCommonPrivateChat(creatorId, partnerId)
                .switchIfEmpty(
                        // 2. Если нет, создаем новый чат
                        Mono.defer(() -> {
                            Chat chat = new Chat();
                            chat.setId(UUID.randomUUID().toString());
                            chat.setChatType(ChatType.PRIVATE);

                            return chatRepository.save(chat)
                                    .flatMap(savedChat -> {
                                        // 3. Создаем записи для обоих участников
                                        ChatMember m1 = createMember(savedChat.getId(), creatorId);
                                        ChatMember m2 = createMember(savedChat.getId(), partnerId);

                                        return chatMemberRepository.saveAll(List.of(m1, m2))
                                                .then(Mono.just(savedChat));
                                    });
                        })
                )
                .map(Chat::getId);
    }

    private ChatMember createMember(String chatId, String userId) {
        ChatMember member = new ChatMember();
        member.setChatId(chatId);
        member.setUserId(userId);
        member.setJoinedAt(LocalDateTime.now());
        return member;
    }



    public Mono<Long> getUnreadMessages(String chatId, String currentUserId) {
        return messageRepository.countUnreadMessages(chatId, currentUserId, MessageState.SENT);
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
}