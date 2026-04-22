package com.FluffyTerror.Umagramm_Backend.responses.media;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UploadInitResponse {
    private String uploadId;      // Уникальный ID сессии загрузки (UUID)
}
