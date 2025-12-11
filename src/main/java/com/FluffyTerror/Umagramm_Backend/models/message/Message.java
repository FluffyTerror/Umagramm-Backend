package com.FluffyTerror.Umagramm_Backend.models.message;

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

    private String chatId;      // FK → Chat

    private String senderId;

    // Это больше не читаемый текст. Это PGP Message (Base64 или ASCII Armor).
    // Серверу все равно, картинка там или текст — это просто набор байт.
    private String content;

    @Column("message_type")
    private MessageType messageType;

    @Column("status")
    private MessageState messageState;
}
