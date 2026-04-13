package com.FluffyTerror.Umagramm_Backend.models.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("attachments")
@Getter
@Setter
public class Attachment {
    @Id
    private Long id;

    private Long messageId;

    private MessageType type; // IMAGE, VIDEO, AUDIO, FILE, etc

    private String url;

    private Instant createdAt;

    private String filename;
}
