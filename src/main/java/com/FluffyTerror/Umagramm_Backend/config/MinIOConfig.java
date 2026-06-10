package com.FluffyTerror.Umagramm_Backend.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIOConfig {
    @Value("${application.minio.endpoint}")
    String minioUrl;
    @Value("${application.minio.access-key}")
    String minioAccessKey;
    @Value("${application.minio.secret-key}")
    String minioSecretKey;
    @Value("${application.minio.bucket-name}")
    String minioBucketName;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(minioAccessKey,minioSecretKey)
                .build();
    }
}
