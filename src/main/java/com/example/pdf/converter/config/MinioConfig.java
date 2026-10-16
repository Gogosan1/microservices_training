package com.example.pdf.converter.config;

import com.example.pdf.converter.config.properties.MinioAppProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioAppProperties appProperties;


    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(appProperties.endpoint())
                .credentials(appProperties.accessKey(), appProperties.secretKey())
                .build();


        try {
            boolean bucketExists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(appProperties.bucket())
                    .build());

            if (!bucketExists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(appProperties.bucket()).build());
            }
        } catch (Exception e) {
            System.err.println("Ошибка инициализации бакета:" + e.getMessage());
        }

        return client;
    }
}
