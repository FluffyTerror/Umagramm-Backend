package com.FluffyTerror.Umagramm_Backend.controller.QRCode;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.requests.qr.QrRequest;
import com.FluffyTerror.Umagramm_Backend.responses.qr.QRResponse;
import com.FluffyTerror.Umagramm_Backend.service.friends.FriendsService;
import com.FluffyTerror.Umagramm_Backend.service.friends.QRService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/qr-code")
@RequiredArgsConstructor
@Tag(name = "QR Code")
public class QRCodeController {
    private final QRService qrService;
    private final FriendsService friendsService;

    @PostMapping("/generate")
    public Mono<QRResponse> generateToken(@AuthenticationPrincipal User consumer) {
        return qrService.generateToken(consumer.getId());
    }

    @PostMapping("/scan")
    public Mono<ResponseEntity<String>> onScanToken(@AuthenticationPrincipal User initiator,
                                                    @RequestBody QrRequest qrRequest) {
        return qrService.consumeToken(qrRequest.token()).flatMap(//--TODO сделать парсинг токена на мобилке
                receiver ->{
                    return friendsService.createFriendsRequest(initiator.getId(),receiver);
                }
        ).thenReturn(ResponseEntity.ok("Friend Request has been sent"));
    }

}
