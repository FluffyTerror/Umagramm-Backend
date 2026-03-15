package com.FluffyTerror.Umagramm_Backend.models.userAuth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table("user_rc_code")
public class UserRecoveryCode {

    @Id
    private String userId; // FK → user.id (1:1)

    private String encryptedPrivateKey;
    private String salt;
    private String recoveryCodeHash;

    private LocalDateTime createdAt;
}
