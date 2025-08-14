package com.jackasher.ageiport.config.filestore;

import com.alibaba.ageiport.ext.file.store.FileStoreOptions;
import com.alibaba.ageiport.ext.file.store.aliyunoss.AliyunOssFileStoreOptions;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.jackasher.ageiport.constant.FileStoreFactoryOptions;
import com.jackasher.ageiport.config.minio.MinioFileStoreOptions;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 文件存储工厂类
 * 根据配置动态创建不同类型的文件存储
 */
@Component
public class FileStoreOptionsFactory {

    private final FileStoreProperties properties;

    // 使用Optional来处理条件性注入的MinioClient
    private final Optional<MinioClient> minioClient;

    private static final Logger log = LoggerFactory.getLogger(FileStoreOptionsFactory.class);

    // 构造函数注入，使用Optional处理可能不存在的MinioClient
    @Autowired
    public FileStoreOptionsFactory(FileStoreProperties properties,
                                   @Autowired(required = false) MinioClient minioClient) {
        this.properties = properties;
        this.minioClient = Optional.ofNullable(minioClient);
    }

    /**
     * 根据配置创建对应的文件存储选项
     * @return 文件存储选项
     */
    public FileStoreOptions createFileStoreOptions() {
        String type = properties.getType();

        log.info("正在创建文件存储，类型: {}", type);

        switch (type.toLowerCase()) {
            case FileStoreFactoryOptions.MINIO:
                return createMinioFileStoreOptions(properties.getMinio());
            case FileStoreFactoryOptions.OSS:
                return createOssFileStoreOptions(properties.getOss());
            default:
                throw new IllegalArgumentException("不支持的文件存储类型: " + type);
        }
    }

    /**
     * 创建MinIO文件存储选项
     */
    private MinioFileStoreOptions createMinioFileStoreOptions(FileStoreProperties.MinioConfig config) {
        log.info("创建MinIO文件存储配置 - endpoint: {}, bucket: {}",
                config.getEndpoint(), config.getBucketName());

        // 检查MinioClient是否可用
        if (!minioClient.isPresent()) {
            throw new IllegalStateException("MinioClient未配置，请检查file-store.type配置是否为minio");
        }

        MinioFileStoreOptions options = new MinioFileStoreOptions();
        options.setEndpoint(config.getEndpoint());
        options.setAccessKey(config.getAccessKey());
        options.setSecretKey(config.getSecretKey());
        options.setBucketName(config.getBucketName());
        options.setMinioClient(minioClient.get());

        return options;
    }

    /**
     * 创建阿里云OSS文件存储选项
     */
    private AliyunOssFileStoreOptions createOssFileStoreOptions(FileStoreProperties.OssConfig config) {
        log.info("创建阿里云OSS文件存储配置 - endpoint: {}, bucket: {}",
                config.getEndpoint(), config.getBucketName());

        AliyunOssFileStoreOptions options = new AliyunOssFileStoreOptions();
        options.setBucketName(config.getBucketName());
        options.setEndpoint(config.getEndpoint());
        options.setConfig(new ClientConfiguration());

        DefaultCredentials credentials = new DefaultCredentials(
                config.getAccessKeyId(),
                config.getAccessKeySecret()
        );
        DefaultCredentialProvider credentialProvider = new DefaultCredentialProvider(credentials);
        options.setCredentialsProvider(credentialProvider);

        return options;
    }
} 