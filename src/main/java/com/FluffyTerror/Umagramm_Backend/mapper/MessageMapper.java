package com.FluffyTerror.Umagramm_Backend.mapper;

import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.repository.attachment.AttachmentRepository;
import com.FluffyTerror.Umagramm_Backend.responses.attachment.AttachmentResponse;
import com.FluffyTerror.Umagramm_Backend.responses.message.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MessageMapper {

    private final AttachmentRepository attachmentRepository;

    public Mono<MessageResponse> toResponse(Message message) {
        return attachmentRepository.findByMessageId(message.getId())
                .map(this::toAttachmentResponse)
                .collectList()
                .map(attachments -> MessageResponse.builder()
                        .id(message.getId())
                        .content(message.getContent())
                        .type(message.getMessageType())
                        .state(message.getMessageState())
                        .senderId(message.getSenderId())
                        .chatId(message.getChatId())
                        .createdAt(message.getCreatedDate())
                        .attachments(attachments)
                        .build()
                );
    }

    private AttachmentResponse toAttachmentResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .type(attachment.getType())
                .url("/api/v1/attachments/" + attachment.getId())// --TODO скорее всего насрано
                .build();
    }
}
