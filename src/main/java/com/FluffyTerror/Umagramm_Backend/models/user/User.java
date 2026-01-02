package com.FluffyTerror.Umagramm_Backend.models.user;

import com.FluffyTerror.Umagramm_Backend.models.attachment.Attachment;
import com.FluffyTerror.Umagramm_Backend.common.BaseAuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("users")
public class User extends BaseAuditingEntity {

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String username;
    private LocalDateTime lastSeen;
    private Long profilePhotoId;   // FK → attachment.id

    @Transient
    private Attachment profilePhoto; // подгружается вручную, НЕ хранится в БД

    @Transient
    public boolean isOnline() {
        return lastSeen != null &&
                lastSeen.isAfter(LocalDateTime.now().minusMinutes(1));
    }
}

