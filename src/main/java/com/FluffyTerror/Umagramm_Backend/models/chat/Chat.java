package com.FluffyTerror.Umagramm_Backend.models.chat;

import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
import com.FluffyTerror.Umagramm_Backend.enums.ChatType;
import com.FluffyTerror.Umagramm_Backend.enums.MessageState;
import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import com.FluffyTerror.Umagramm_Backend.models.message.Message;
import com.FluffyTerror.Umagramm_Backend.models.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("chats")
public class Chat extends BaseAuditingEntity implements Persistable<String> {

    @Id
    private String id;

    private String title; // Название для групп, null для личек

    private Long avatarId; // Общий аватар для группы

    @Transient
    private boolean isNew = false; // По умолчанию false

    @Column("type")
    private ChatType chatType;

    private Integer keyVersion = 1;

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    // Метод для удобства
    public void markNew() {
        this.isNew = true;
    }
}
