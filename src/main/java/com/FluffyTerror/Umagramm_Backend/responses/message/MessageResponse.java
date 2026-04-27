package com.FluffyTerror.Umagramm_Backend.responses.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.responses.attachment.AttachmentResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageResponse {
    private Long id;
    private String content;
    private MessageType type;
    private MessageState state;
    private String senderId;
    private String chatId;
    private LocalDateTime createdAt;
    private String initialisationVector;
    private Long keyVersion;
    private List<AttachmentResponse> attachments;
}
