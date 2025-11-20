package com.FluffyTerror.Umagramm_Backend.models.user;

import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("group_user")
public class GroupUser extends BaseAuditingEntity {
    @Id
    private Long id;

    private Long groupChatId;

    private String userId;

    private Long profilePhotoId;   // FK → attachment.id

    @Transient
    private Attachment profilePhoto; // подгружается вручную, НЕ хранится в БД

    private boolean isAdmin;

    private LocalDateTime joinedAt;
}
