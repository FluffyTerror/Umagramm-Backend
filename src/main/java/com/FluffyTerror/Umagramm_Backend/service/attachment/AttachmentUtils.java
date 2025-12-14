package com.FluffyTerror.Umagramm_Backend.service.attachment;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Slf4j
public class AttachmentUtils {

    private AttachmentUtils() {
    }

    public static byte[] readFileFromLocation(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return new byte[0];
        }
        try {
            Path file = new File(fileUrl).toPath();
            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.warn("No file found by the path {}", fileUrl);
        }
        return new byte[0];
    }
}
