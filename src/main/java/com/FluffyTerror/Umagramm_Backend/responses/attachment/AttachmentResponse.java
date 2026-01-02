package com.FluffyTerror.Umagramm_Backend.responses.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AttachmentResponse {
    private Long id;
    private MessageType type;
    private String url;
}