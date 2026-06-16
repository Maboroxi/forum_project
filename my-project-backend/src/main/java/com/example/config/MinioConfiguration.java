package com.example.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfiguration {

    @Value("${spring.minio.endpoint}")
    String endpoint;
    @Value("${spring.minio.username}")
    String username;
    @Value("${spring.minio.password}")
    String password;

    @Bean
    public MinioClient minioClient(){
        log.info("Init minio client...");
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(username, password)
                .build();
    }

    /**
     * 启动时自动检查并创建 MinIO 的 study 桶，避免手动操作
     */
    @Bean
    public InitializingBean minioBucketInit(MinioClient minioClient) {
        return () -> {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket("study").build());
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket("study").build());
                    log.info("MinIO bucket 'study' created automatically");
                } else {
                    log.info("MinIO bucket 'study' already exists");
                }
            } catch (Exception e) {
                log.error("Failed to auto-create MinIO bucket 'study': {}", e.getMessage());
            }
        };
    }
}
