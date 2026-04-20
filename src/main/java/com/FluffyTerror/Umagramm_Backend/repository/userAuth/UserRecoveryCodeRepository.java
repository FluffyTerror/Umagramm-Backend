package com.FluffyTerror.Umagramm_Backend.repository.userAuth;

import com.FluffyTerror.Umagramm_Backend.models.userAuth.UserRecoveryCode;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRecoveryCodeRepository extends ReactiveCrudRepository<UserRecoveryCode,String> {
    @Modifying
    @Query("UPDATE recovery_code SET encrypted_private_key = :encryptedKey, salt = :salt, nonce = :nonce WHERE user_id = :userId")
    Mono<Integer> updateVaultData(String userId, String encryptedKey, String salt,String nonce);
}
