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
    private final AttachmentRepository attachmentRepository;
    private final MessageMapper mapper;
    private final AttachmentService attachmentService;
    private final NotificationService notificationService;
    private final ChatService chatService;

    public Mono<Void> saveMessage(MessageRequest messageRequest) {

        chatRepository.findById(messageRequest.getChatId()).switchIfEmpty(Mono.error(new Exception(String.format("Chat with id %s not found!", messageRequest.getChatId())))).flatMap(chat -> {
            Message message = new Message();
            message.setContent(messageRequest.getContent());
            message.setChatId(chat.getId());
            message.setSenderId(chat.getSenderId());
            message.setMessageType(messageRequest.getType());
            message.setMessageState(MessageState.SENT);
            return messageRepository.save(message)
                    .flatMap(saved -> {
                        String recipient = chat.getRecipientId();
                        return chatService.getChatName(chat.getId(), chat.getSenderId())
                                .doOnNext(chatName ->
                                {

                                    Notification notification = Notification.builder().chatId(chat.getId())
                                            .messageType(saved.getMessageType())
                                            .content(saved.getContent())
                                            .senderId(chat.getSenderId())
                                            .type(NotificationType.MESSAGE)
                                            .chatName(chatName)
                                            .build();
                                    notificationService.sendNotification(recipient, notification);
                                }).then();

                    });

            //--TODO не указываю ReceiverId - возможно будут проблемы из за этого
        });

        return Mono.empty().then();
    }

    public Mono<List<MessageResponse>> findMessages(String chatId) {
        return messageRepository.findMessagesByChatId(chatId).flatMap(mapper::toResponse).collectList();
    }


    @Transactional //ставим аннотацию так как метод setMessagesToSeenByChat имеет аннотацию @Modifying
    public Mono<Void> setMessagesToSeen(String chatId, Authentication authentication) {
        return chatRepository.findById(chatId).switchIfEmpty(Mono.error(new Exception(String.format("Chat with id %s not found!", chatId)))).flatMap(chat -> {
            final String recipientId = getRecipientId(chat, authentication);
            return messageRepository.setMessagesToSeenByChat(MessageState.SEEN.name(), chat.getId()).thenReturn(Tuples.of(chat, recipientId));
        }).doOnNext(tuple -> {
            Chat chat = tuple.getT1();
            String recipientId = tuple.getT2();
            Notification notification = Notification.builder()
                    .chatId(chat.getId())
                    .type(NotificationType.SEEN)
                    .senderId(chat.getSenderId())
                    .build();
            notificationService.sendNotification(recipientId, notification);
        }).then();

    }

    public Mono<Void> uploadMediaMessage(
            String chatId,
            FilePart file,
            Authentication authentication
    ) {
        return chatRepository.findById(chatId)
                .switchIfEmpty(Mono.error(
                        new Exception(String.format("Chat with id %s not found!", chatId))
                ))
                .flatMap(chat -> {
                    final String recipientId = getRecipientId(chat, authentication);
                    final String senderId = getSenderId(chat, authentication);

                    return attachmentService.saveFile(file, senderId)
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
                                                    .doOnNext(savedAttachment -> {

                                                        Notification notification = Notification.builder()
                                                                .chatId(chat.getId())
                                                                .senderId(senderId)
                                                                .messageType(MessageType.IMAGE)
                                                                .type(NotificationType.MESSAGE)
                                                                .attachments(List.of(
                                                                        AttachmentResponse.builder()
                                                                                .id(savedAttachment.getId())
                                                                                .type(savedAttachment.getType())
                                                                                .url(savedAttachment.getUrl())
                                                                                .build())).build();


                                                        notificationService.sendNotification(
                                                                recipientId,
                                                                notification
                                                        );
                                                    });
                                        });
                            });
                })
                .then();
    }


    private String getSenderId(Chat chat, Authentication authentication) {
        if (chat.getSenderId().equals(authentication.getName())) {
            return chat.getSenderId();
        }
        return chat.getRecipientId();
    }


    private String getRecipientId(Chat chat, Authentication authentication) {
        if (chat.getSenderId().equals(authentication.getName())) {
            return chat.getRecipientId();
        }
        return chat.getSenderId();
    }
}
