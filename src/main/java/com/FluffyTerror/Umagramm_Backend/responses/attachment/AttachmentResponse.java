package com.FluffyTerror.Umagramm_Backend.responses.attachment;

import com.FluffyTerror.Umagramm_Backend.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private MessageType type;
    private String url;
}