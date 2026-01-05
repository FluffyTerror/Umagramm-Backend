package com.FluffyTerror.Umagramm_Backend.models.chatMember;

import com.FluffyTerror.Umagramm_Backend.enums.MemberRole;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("chat_members")
@Getter
@Setter
public class ChatMember {
    @Id
    private Long id;
    private String chatId;
    private String userId;
    private MemberRole role; // ADMIN (может удалять), MEMBER (просто пишет)
    private LocalDateTime joinedAt;
}
