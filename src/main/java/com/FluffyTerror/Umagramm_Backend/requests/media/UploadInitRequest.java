package com.FluffyTerror.Umagramm_Backend.requests.media;

import lombok.Data;

@Data
public class UploadInitRequest {
    private String filename;      // Оригинальное имя файла (например, "video.mp4")
    private long fileSize;        // Общий размер в байтах (чтобы сервер знал, хватит ли места)
    private int totalChunks;      // На сколько кусочков Android порезал файл
    private String contentType;   // Тип файла (video/mp4, image/jpeg)
}
