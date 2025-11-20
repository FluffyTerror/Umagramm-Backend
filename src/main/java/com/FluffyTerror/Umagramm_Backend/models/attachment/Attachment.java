package com.FluffyTerror.Umagramm_Backend.models.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("attachments")
public class Attachment {
    @Id
    private Long id;

    private Long messageId;

    private MessageType type; // IMAGE, VIDEO, AUDIO, FILE, etc

    private String url;

    private Instant createdAt;
}
