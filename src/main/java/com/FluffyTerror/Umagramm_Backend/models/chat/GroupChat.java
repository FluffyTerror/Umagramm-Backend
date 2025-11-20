package com.FluffyTerror.Umagramm_Backend.models.chat;

import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("group_chat")
public class GroupChat extends BaseAuditingEntity {

    @Id
    private Long id;

    private String title;
    private String description;
    private String ownerId;
    private Long chatPhotoId;          // FK → attachment
    private boolean isPublic;
    private String inviteLink;
    private Integer membersCount;      // кеш
    private Long lastMessageId;        // кеш
    private LocalDateTime lastMessageTime; // кеш
}

