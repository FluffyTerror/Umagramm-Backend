package com.FluffyTerror.Umagramm_Backend.models.friends;

import com.FluffyTerror.Umagramm_Backend.enums.FriendStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("friends")
public class Friend {
    @Id
    private String id;
    private String senderId;
    private String receiverId;
    private FriendStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
