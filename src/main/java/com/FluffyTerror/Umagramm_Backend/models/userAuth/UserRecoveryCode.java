package com.FluffyTerror.Umagramm_Backend.models.userAuth;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;


@Data
@Table("recovery_code")
public class UserRecoveryCode implements Persistable<String> {

    @Id
    private String userId; // FK → user.id (1:1)

    private String recoveryCodeHash;

    private String salt;

    private String encryptedPrivateKey;

    @Override
    public String getId() {
        return userId;
    }

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }
}
