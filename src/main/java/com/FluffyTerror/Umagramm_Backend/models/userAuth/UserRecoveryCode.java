package com.FluffyTerror.Umagramm_Backend.models.userAuth;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("user_auth")
public class UserRecoveryCode {

    @Id
    private String userId; // FK → user.id (1:1)

    private String recoveryCodeHash;

    private LocalDateTime lastLogin;
}
