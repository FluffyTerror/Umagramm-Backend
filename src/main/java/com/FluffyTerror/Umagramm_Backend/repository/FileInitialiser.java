package com.FluffyTerror.Umagramm_Backend.repository;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileInitialiser implements CommandLineRunner {

    @Value("${application.file.uploads.media-output-path}")
    private String uploadPath;

    private final DatabaseClient databaseClient; // Для работы с БД

    @Override
    public void run(String... args) {
        initializePhysicalFile();
        initializeDatabaseEntry().subscribe(); // Запускаем реактивную цепочку
    }

    private void initializePhysicalFile() {
        try {
            Path targetPath = Paths.get(uploadPath, "default-avatar.jpg");
            if (Files.notExists(targetPath)) {
                Files.createDirectories(targetPath.getParent());
                ClassPathResource resource = new ClassPathResource("static/images/default-avatar.jpg");
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("Дефолтная аватарка скопирована в: {}", targetPath);
                }
            }
        } catch (IOException e) {
            log.error("Ошибка при копировании файла: {}", e.getMessage());
        }
    }

    private Mono<Void> initializeDatabaseEntry() {
        return databaseClient.sql("SELECT COUNT(*) FROM attachments WHERE id = 1")
                .map(row -> row.get(0, Long.class))
                .one()
                .flatMap(count -> {
                    if (count != null && count == 0) {
                        log.info("Default avatar does not exist, creating..");
                        return databaseClient.sql("""
                                INSERT INTO attachments (id,message_id,type, url, created_at)
                                VALUES (1, null, 'IMAGE', './uploads/default-avatar.jpg', NOW())
                            """)
                                .then()
                                .doOnSuccess(v -> log.info("default avatar created!"));
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("DB sucked ass: {}", e.getMessage());
                    return Mono.empty();
                });
    }
}
