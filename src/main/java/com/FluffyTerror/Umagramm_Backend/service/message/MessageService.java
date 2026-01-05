package com.FluffyTerror.Umagramm_Backend.service.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.enums.NotificationType;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.notification.Notification;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.chatMember.ChatMemberRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.responses.attachment.AttachmentResponse;
import com.FluffyTerror.Umagramm_Backend.responses.message.MessageResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import com.FluffyTerror.Umagramm_Backend.service.chat.ChatService;
import com.FluffyTerror.Umagramm_Backend.mapper.MessageMapper;
import com.FluffyTerror.Umagramm_Backend.service.notification.NotificationService;
import com.FluffyTerror.Umagramm_Backend.requests.message.MessageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository; // Добавляем
    private final AttachmentRepository attachmentRepository;
    private final MessageMapper mapper;
    private final AttachmentService attachmentService;
    private final NotificationService notificationService;
    private final ChatService chatService;

    /**
     * Сохранение текстового сообщения
     */
    public Mono<Void> saveMessage(MessageRequest messageRequest, Authentication authentication) {
        String senderId = authentication.getName(); // Берем ID отправителя из токена

        return chatRepository.findById(messageRequest.getChatId())
                .switchIfEmpty(Mono.error(new Exception("Chat not found")))
                .flatMap(chat -> {
                    Message message = new Message();
                    message.setContent(messageRequest.getContent());
                    message.setChatId(chat.getId());
                    message.setSenderId(senderId);
                    message.setMessageType(messageRequest.getType());
                    message.setMessageState(MessageState.SENT);

                    return messageRepository.save(message)
                            .flatMap(saved -> notifyAllChatMembers(saved, chat, senderId));
                });
    }

    /**
     * Вспомогательный метод для рассылки уведомлений всем участникам чата
     */
    private Mono<Void> notifyAllChatMembers(Message message, Chat chat, String senderId) {
        return chatService.getChatName(chat, senderId)
                .flatMapMany(chatName ->
                        chatMemberRepository.findAllByChatId(chat.getId())
                                .filter(member -> !member.getUserId().equals(senderId)) // Не шлем самому себе
                                .flatMap(member -> {
                                    Notification notification = Notification.builder()
                                            .chatId(chat.getId())
                                            .messageType(message.getMessageType())
                                            .content(message.getContent())
                                            .senderId(senderId)
                                            .type(NotificationType.MESSAGE)
                                            .chatName(chatName)
                                            .build();

                                    // Отправляем уведомление конкретному участнику
                                    return notificationService.sendNotification(member.getUserId(), notification);
                                })
                ).then();
    }

    /**
     * Загрузка медиа-сообщения
     */
    public Mono<Void> uploadMediaMessage(String chatId, FilePart file, Authentication authentication) {
        String senderId = authentication.getName();

        return chatRepository.findById(chatId)
                .switchIfEmpty(Mono.error(new Exception("Chat not found")))
                .flatMap(chat -> attachmentService.saveFile(file, senderId)
                        .flatMap(filePath -> {
                            Message message = new Message();
                            message.setChatId(chat.getId());
                            message.setSenderId(senderId);
                            message.setMessageType(MessageType.IMAGE);
                            message.setMessageState(MessageState.SENT);

                            return messageRepository.save(message)
                                    .flatMap(savedMessage -> {
                                        Attachment attachment = new Attachment();
                                        attachment.setMessageId(savedMessage.getId());
                                        attachment.setType(MessageType.IMAGE);
                                        attachment.setUrl(filePath);

                                        return attachmentRepository.save(attachment)
                                                .then(notifyAllChatMembers(savedMessage, chat, senderId));
                                    });
                        })
                ).then();
    }

    /**
     * Прочтение сообщений
     */
    public Mono<Void> setMessagesToSeen(String chatId, Authentication authentication) {
        String currentUserId = authentication.getName();

        return messageRepository.setMessagesToSeenByChat(
                MessageState.SEEN, // Новый статус
                MessageState.SENT, // Какой статус меняем
                chatId,
                currentUserId
        ).then(/* логика уведомлений */);
    }

    public Mono<List<MessageResponse>> findMessages(String chatId) {
        return messageRepository.findMessagesByChatId(chatId)
                .flatMap(mapper::toResponse)
                .collectList();
    }
}