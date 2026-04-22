package com.FluffyTerror.Umagramm_Backend.controller.friends;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.requests.friends.FriendRequest;
import com.FluffyTerror.Umagramm_Backend.responses.friend.FriendResponse;
import com.FluffyTerror.Umagramm_Backend.service.friends.FriendsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/friend-requests")
@RequiredArgsConstructor
@Tag(name = "Friend-requests")
public class FriendsController {
    private final FriendsService friendsService;

    @PostMapping("/accept")
    public Mono<ResponseEntity<String>> acceptFriendRequest(@AuthenticationPrincipal User receiver,  @RequestBody FriendRequest request) {
        return friendsService.acceptFriendRequest(request.senderId(),receiver.getId()).thenReturn(ResponseEntity.ok("You are friends now !"));
    }

    @PostMapping("/decline")
    public Mono<ResponseEntity<String>> declineFriendRequest(@AuthenticationPrincipal User receiver, @RequestBody FriendRequest request) {
        return friendsService.declineFriendRequest(request.senderId(),receiver.getId()).thenReturn(ResponseEntity.ok("You declined friend request"));
    }

    @GetMapping("/all-pending-requests")
    public Flux<FriendResponse> findAllIncomingRequests(@AuthenticationPrincipal User receiver) {
        return friendsService.getAllIncomingRequests(receiver.getId());
    }

}
