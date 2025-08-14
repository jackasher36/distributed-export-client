package com.jackasher.ageiport.config.minio;

import com.jackasher.ageiport.config.filestore.FileStoreProperties;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * @author Jackasher
 * @version 1.0
 * @className MinioClientConfig
 * @since 1.0
 **/
@Configuration
public class MinioClientConfig {


    @Resource
    private FileStoreProperties fileStoreProperties;

    @Bean
    @ConditionalOnProperty(name = "ageiport.file-store.type", havingValue = "minio", matchIfMissing = false)
    public MinioClient minioClient() {
        FileStoreProperties.MinioConfig minioConfig = fileStoreProperties.getMinio();

        return MinioClient.builder()
                .endpoint(minioConfig.getEndpoint())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();
    }
}
