package com.FluffyTerror.Umagramm_Backend.service.user;

import com.FluffyTerror.Umagramm_Backend.repository.user.UserRepository;
import com.FluffyTerror.Umagramm_Backend.mapper.UserMapper;
import com.FluffyTerror.Umagramm_Backend.responses.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Mono<List<UserResponse>> getAllUsersExceptSelf(Authentication authentication) {
        return userRepository.getUsersExceptSelf(authentication.getName()).map(userMapper::toUserResponse).collectList();
    }


}
