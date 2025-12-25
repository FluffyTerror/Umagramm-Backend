package com.FluffyTerror.Umagramm_Backend.notification;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.enums.NotificationType;
import com.FluffyTerror.Umagramm_Backend.service.attachment.AttachmentResponse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    private String chatId;
    private String content;
    private String senderId;
    private String chatName;
    private MessageType messageType;
    private NotificationType type;
    private List<AttachmentResponse> attachments; //тут писали что нужно делать byte[] для медиа - но это же хуйня?
}
