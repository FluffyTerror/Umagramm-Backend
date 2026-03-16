package com.FluffyTerror.Umagramm_Backend.controller.recovery;

import com.FluffyTerror.Umagramm_Backend.requests.recovery.FinishRecoveryCodeRequest;
import com.FluffyTerror.Umagramm_Backend.requests.recovery.RestoreAccountRequest;
import com.FluffyTerror.Umagramm_Backend.responses.recovery.RestoreResponse;
import com.FluffyTerror.Umagramm_Backend.service.RecoveryCode.RecoveryCodeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/recovery")
@RequiredArgsConstructor
@Tag(name = "Recovery")
public class RecoveryController {
    private final RecoveryCodeService recoveryCodeService;

    @PutMapping("/finish-recovery-code")
    public Mono<ResponseEntity<Void>> completeRecoveryCode(@AuthenticationPrincipal Principal principal, @RequestBody FinishRecoveryCodeRequest request) {
        return recoveryCodeService.finishRecoveryCode(principal.getName(), request).thenReturn(ResponseEntity.ok().build());
    }

    @PostMapping("/restore-access")
    public Mono<RestoreResponse> restore(@RequestBody RestoreAccountRequest request) {
        return recoveryCodeService.restoreAccount(request);
    }

}
