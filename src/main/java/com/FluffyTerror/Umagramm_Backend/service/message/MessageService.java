package com.FluffyTerror.Umagramm_Backend.service.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.enums.NotificationType;
import com.FluffyTerror.Umagramm_Backend.mapper.MessageMapper;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.notification.Notification;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.chatMember.ChatMemberRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.requests.message.MessageRequest;
import com.FluffyTerror.Umagramm_Backend.requests.message.MessageTextRequest;
import com.FluffyTerror.Umagramm_Backend.responses.attachment.AttachmentResponse;
import com.FluffyTerror.Umagramm_Backend.responses.message.MessageResponse;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import com.FluffyTerror.Umagramm_Backend.service.chat.ChatService;
import com.FluffyTerror.Umagramm_Backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.net.URLConnection;
import java.util.List;


@Service
@Slf4j
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
    public Mono<MessageResponse> saveMessage(MessageRequest messageRequest, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        String senderId = user.getId();
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
                            .flatMap(saved ->
                                    notifyAllChatMembers(saved, chat, senderId, null)
                                            // Заменяем thenReturn на then!
                                            .then(mapper.toResponse(saved))
                            );
                });
    }

    /**
     * Вспомогательный метод для рассылки уведомлений всем участникам чата
     */
    private Mono<Void> notifyAllChatMembers(Message message, Chat chat, String senderId, List<AttachmentResponse> attachments) {
        return chatService.getChatName(chat, senderId)
                .flatMapMany(chatName ->
                        chatMemberRepository.findAllByChatId(chat.getId())
                                //.filter(member -> !member.getUserId().equals(senderId)) // Не шлем самому себе
                                .flatMap(member -> {
                                    Notification notification = Notification.builder()
                                            .chatId(chat.getId())
                                            .messageType(message.getMessageType())
                                            .content(message.getContent())
                                            .senderId(senderId)
                                            .type(NotificationType.MESSAGE)
                                            .chatName(chatName)
                                            .attachments(attachments)
                                            .messageId(message.getId())
                                            .build();

                                    // Отправляем уведомление конкретному участнику
                                    return notificationService.sendNotification(member.getUserId(), notification);
                                })
                ).then();
    }

    /**
     * Загрузка медиа-сообщения
     */
    public Mono<Void> uploadMediaMessage(String chatId, Flux<FilePart> files, Authentication authentication, MessageTextRequest messageTextRequest) {
        User user = (User) authentication.getPrincipal();
        String senderId = user.getId();

        return chatRepository.findById(chatId)
                .switchIfEmpty(Mono.error(new Exception("Chat not found")))
                .flatMap(chat -> {
                    Message message = new Message();
                    message.setChatId(chat.getId());
                    message.setSenderId(senderId);
                    message.setContent(messageTextRequest.getContent());
                    if (messageTextRequest.getContent() == null || messageTextRequest.getContent().trim().isEmpty()) {
                        message.setMessageType(MessageType.IMAGE);
                    } else {
                        message.setMessageType(MessageType.MIXED);
                    }
                    message.setMessageState(MessageState.SENT);

                    return messageRepository.save(message)
                            .flatMap(savedMessage ->
                                    files.flatMap(file -> {
                                                String originalFilename = file.filename();
                                                String contentType = file.headers().getContentType().toString();

                                                MessageType detectedType = determineMessageType(originalFilename);

                                                String finalContentType = contentType;
                                                return attachmentService.saveFile(file, senderId)
                                                        .flatMap(filePath -> {
                                                            Attachment attachment = new Attachment();
                                                            attachment.setMessageId(savedMessage.getId());
                                                            attachment.setType(detectedType);
                                                            attachment.setUrl(filePath);
                                                            attachment.setContentType(finalContentType);
                                                            attachment.setFilename(originalFilename);
                                                            return attachmentRepository.save(attachment);
                                                        });
                                            })
                                            .map(savedAt -> new AttachmentResponse(
                                                    savedAt.getId(),
                                                    savedAt.getType(),
                                                    savedAt.getUrl(),
                                                    savedAt.getFilename()
                                            ))
                                            .collectList()
                                            // 4. Когда ВСЕ картинки сохранены, отправляем ОДНО уведомление со списком
                                            .flatMap(attachments -> notifyAllChatMembers(savedMessage, chat, senderId, attachments))
                            );
                }).then();
    }
    private MessageType determineMessageType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp" -> MessageType.IMAGE;
            case "mp4", "mov", "avi", "mkv" -> MessageType.VIDEO;
            case "mp3", "wav", "flac", "aac" -> MessageType.AUDIO;
            case "pdf", "doc", "docx", "txt", "xlsx" -> MessageType.DOCUMENT;
            default -> MessageType.FILE; // Прочие файлы
        };
    }

    /**
     * Прочтение сообщений
     */
    public Mono<Void> setMessagesToSeen(String chatId, Long lastMessageId, String currentUserId) {

        return messageRepository.setMessagesToSeenByChat(
                        MessageState.SEEN,
                        MessageState.SENT,
                        chatId,
                        lastMessageId,
                        currentUserId
                )
                .flatMap(updatedCount -> {
                    if (updatedCount > 0) {
                        log.info("Read msg: {} in chat {}", updatedCount, chatId);

                        // сигнал в сокет
                        return notifyAboutReadStatus(chatId, lastMessageId, currentUserId);
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> notifyAboutReadStatus(String chatId, Long lastMessageId, String readerId) {
        return chatMemberRepository.findAllByChatId(chatId)
                .flatMap(member -> {
                    Notification readNotification = Notification.builder()
                            .type(NotificationType.SEEN)
                            .chatId(chatId)
                            .content(lastMessageId.toString())
                            .senderId(readerId)
                            .build();

                    return notificationService.sendNotification(member.getUserId(), readNotification);
                })
                .then();
    }

    public Mono<List<MessageResponse>> findMessages(String chatId) {
        return messageRepository.findMessagesByChatId(chatId)
                .flatMapSequential(mapper::toResponse)
                .collectList();
    }
}