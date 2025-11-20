package com.FluffyTerror.Umagramm_Backend.models.message;

import com.FluffyTerror.Umagramm_Backend.models.chat.Chat;
import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("messages")
public class Message extends BaseAuditingEntity {

    @Id
    private Long id;

    private Long chatId;      // FK → Chat
    private Long groupChatId; // FK → GroupChat
    private String senderId;

    private String content;
    private MessageType messageType;
    private MessageState messageState;
}
