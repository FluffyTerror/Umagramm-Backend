package com.FluffyTerror.Umagramm_Backend.requests.media;


import lombok.Data;

@Data
public class FinishUploadRequest {
    private String chatId;           // В какой чат отправляем медиа-сообщение
    private String messageText;      // Описание к файлу (если есть)
    // Можно добавить хэш-сумму всего файла для финальной проверки целостности
    private String fileHash;
}
