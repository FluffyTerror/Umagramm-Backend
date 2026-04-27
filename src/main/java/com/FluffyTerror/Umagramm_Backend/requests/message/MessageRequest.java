package com.FluffyTerror.Umagramm_Backend.requests.message;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageRequest {

    private String content;
    private String senderId;
    private MessageType type;
    private String chatId;
    private String initialisationVector;
    private Long keyVersion;

}
