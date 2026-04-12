package com.FluffyTerror.Umagramm_Backend.responses.user;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private LocalDateTime lastSeen;
    private Long profilePhotoId;
    private boolean isOnline;
}
