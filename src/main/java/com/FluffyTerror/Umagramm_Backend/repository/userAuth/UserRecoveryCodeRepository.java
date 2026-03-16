package com.FluffyTerror.Umagramm_Backend.repository.userAuth;

import com.FluffyTerror.Umagramm_Backend.models.userAuth.UserRecoveryCode;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRecoveryCodeRepository extends ReactiveCrudRepository<UserRecoveryCode,String> {
}
