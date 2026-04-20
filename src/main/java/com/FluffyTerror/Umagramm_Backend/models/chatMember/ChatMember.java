package com.FluffyTerror.Umagramm_Backend.models.chatMember;

import com.FluffyTerror.Umagramm_Backend.enums.MemberRole;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("chat_members")
@Getter
@Setter
public class ChatMember implements Persistable<Long> {
    @Id
    private Long id;
    private String chatId;
    private String userId;
    @Column("member_role")
    private MemberRole role; // ADMIN (может удалять), MEMBER (просто пишет)
    private String invitedBy;
    private LocalDateTime joinedAt;

    @Transient
    private boolean isNew = true; // Для участников чата мы всегда ставим true при создании

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }
}
