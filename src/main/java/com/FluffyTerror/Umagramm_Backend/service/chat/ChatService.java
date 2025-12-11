package com.FluffyTerror.Umagramm_Backend.service.chat;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.service.mapper.ChatMapper;
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

    public Mono<List<ChatResponse>> getChatByReceiverId(Authentication currentUser) {
        String userId = currentUser.getName();
        return chatRepository.findAllChatsBySenderId(userId)
                .flatMap(chat -> chatMapper.toChatResponse(chat, userId)).collectList();
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
