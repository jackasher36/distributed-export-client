package com.jackasher.ageiport.config.filestore;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置类
 * 支持动态选择不同的文件存储类型
 */
@Component
@ConfigurationProperties(prefix = "ageiport.file-store")
@Data
public class FileStoreProperties {
    
    /**
     * 文件存储类型：minio、oss、local等
     */
    private String type = "minio";
    
    /**
     * MinIO配置
     */
    private MinioConfig minio = new MinioConfig();
    
    /**
     * 阿里云OSS配置
     */
    private OssConfig oss = new OssConfig();


    @Data
    public static class MinioConfig {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketName;

    }

    @Data
    public static class OssConfig {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName;

    }
} 