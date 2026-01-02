package com.FluffyTerror.Umagramm_Backend.service.chat;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.responses.chat.ChatResponse;
import com.FluffyTerror.Umagramm_Backend.mapper.ChatMapper;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    // Пример метода, который возвращает обогащенный ChatResponse
    public Mono<ChatResponse> getChatResponse(String chatId, String currentUserId) {
        return chatRepository.findById(chatId)
                .flatMap(chat -> {
                    // Вычисляем ID собеседника
                    String recipientId = chat.getSenderId().equals(currentUserId)
                            ? chat.getRecipientId()
                            : chat.getSenderId();

                    // Здесь собираем все данные параллельно (как было у вас в маппере)
                    return Mono.zip(
                            getChatName(chat.getId(), currentUserId),        // T1
                            getUnreadMessages(chat.getId(), currentUserId),  // T2
                            getLastMessage(chat.getId()),                    // T3
                            isRecipientOnline(recipientId)                   // T4
                    ).map(tuple ->
                            // И вот тут вызываем "чистый" маппер
                            chatMapper.toChatResponse(
                                    chat,          // Сам чат
                                    tuple.getT1(), // name
                                    tuple.getT2(), // unread
                                    tuple.getT3(), // lastMsg
                                    tuple.getT4()  // online
                            )
                    );
                });
    }

    public Mono<List<ChatResponse>> getChatByReceiverId(Authentication currentUser) {
        String currentUserId = currentUser.getName();

        return chatRepository.findAllChatsBySenderId(currentUserId)
                // Просто вызываем наш универсальный метод для каждого чата
                .flatMap(chat -> enrichChatWithDetails(chat, currentUserId))
                .collectList();
    }

    // Вспомогательный метод: берет "голый" Чат и делает из него полный ChatResponse
    private Mono<ChatResponse> enrichChatWithDetails(Chat chat, String currentUserId) {
        String recipientId = chat.getSenderId().equals(currentUserId)
                ? chat.getRecipientId()
                : chat.getSenderId();


        return Mono.zip(
                getChatName(chat.getId(), currentUserId),
                getUnreadMessages(chat.getId(), currentUserId),
                getLastMessage(chat.getId()),
                isRecipientOnline(recipientId)
        ).map(tuple -> chatMapper.toChatResponse(
                chat,
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4()
        ));
    }

    public Mono<String> getChatName(String chatId, String currentUserId) {
        return chatRepository.findById(chatId)
                .flatMap(chat ->
                        Mono.zip(
                                userRepository.getUserById(chat.getSenderId()),
                                userRepository.getUserById(chat.getRecipientId())
                        ).map(tuple -> {
                            User sender = tuple.getT1();
                            User recipient = tuple.getT2();

                            if (recipient.getId().equals(currentUserId)) {
                                return sender.getFirstName() + " " + sender.getLastName();
                            }
                            return recipient.getFirstName() + " " + recipient.getLastName();
                        })
                );
    }


    /**
     * Возвращает количество непрочитанных сообщений для чата, где текущий пользователь - получатель.
     *
     * @param messageChatId ID чата (Long, из сущности Message)
     * @param currentUserId ID текущего пользователя (String)
     * @return Mono с количеством непрочитанных сообщений
     */
    public Mono<Long> getUnreadMessages(String messageChatId, String currentUserId) {

        return messageRepository.findAllUnreadMessages(messageChatId, currentUserId, MessageState.SENT)
                .count();
    }


    public Mono<Message> getLastMessage(String chatId) {
        // вся проверка и обработка в маппере
        return messageRepository.findLastMessageByChatId(chatId);
    }

    /**
     * Возвращает время создания последнего сообщения в чате.
     *
     * @param messageChatId ID чата (Long, из сущности Message)
     * @return Mono с LocalDateTime или null
     */
    public Mono<LocalDateTime> getLastMessageTime(String messageChatId) {
        return messageRepository.findLastMessageByChatId(messageChatId)
                .map(Message::getCreatedDate)
                .defaultIfEmpty(null);
    }

    public Mono<Boolean> isRecipientOnline(String recipientId) {
        return userRepository.findById(recipientId)
                .map(User::isOnline)
                .defaultIfEmpty(false);
    }

    public Mono<String> createChat(String senderId, String recipientId) {
        Mono<User> sender = userRepository.getUserById(senderId);
        Mono<User> recipient = userRepository.getUserById(recipientId);
        return chatRepository.findChatBySenderIdAndRecipientId(senderId, recipientId).map(
                Chat::getId
        ).switchIfEmpty(
                Mono.zip(sender, recipient).flatMap(
                        tuple -> {
                            User pSender = tuple.getT1();
                            User pRecipient = tuple.getT2();
                            Chat chat = new Chat();
                            chat.setSenderId(pSender.getId());
                            chat.setRecipientId(pRecipient.getId());
                            return chatRepository.save(chat).map(Chat::getId);

                        }
                )
        );
    }
}
