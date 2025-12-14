package com.FluffyTerror.Umagramm_Backend.service.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.repository.chat.ChatRepository;
import com.FluffyTerror.Umagramm_Backend.repository.message.MessageRepository;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentService;
import com.FluffyTerror.Umagramm_Backend.service.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageMapper mapper;
    private final AttachmentService attachmentService;

    public Mono<Void> saveMessage(MessageRequest messageRequest) {

        chatRepository.findById(messageRequest.getChatId()).switchIfEmpty(Mono.error(new Exception(String.format("Chat with id %s not found!", messageRequest.getChatId())))).flatMap(chat -> {
            Message message = new Message();
            message.setContent(messageRequest.getContent());
            message.setChatId(chat.getId());
            message.setSenderId(chat.getSenderId());
            message.setMessageType(messageRequest.getType());
            message.setMessageState(MessageState.SENT);
            return messageRepository.save(message);
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
            return messageRepository.setMessagesToSeenByChat(MessageState.SEEN.name(), chat.getId());
        }).then();
        //--TODO notification

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
                                message.setContent(filePath);
                                return messageRepository.save(message);
                            });

                    //--TODO notification
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
