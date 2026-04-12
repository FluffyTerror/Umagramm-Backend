package com.FluffyTerror.Umagramm_Backend.mapper;

import com.FluffyTerror.Umagramm_Backend.models.user.User;
import com.FluffyTerror.Umagramm_Backend.responses.user.UserResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;


@Service
public class UserMapper {
    public UserResponse toUserResponse (User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .lastSeen(user.getLastSeen())
                .profilePhotoId(user.getProfilePhotoId())
                .isOnline(user.isOnline())
                .build();
    }
}
