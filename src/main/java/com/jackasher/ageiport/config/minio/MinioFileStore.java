// src/main/java/com/alibaba/ageiport/ext/file/store/minio/MinioFileStore.java
package com.jackasher.ageiport.config.minio;

import com.alibaba.ageiport.ext.file.store.FileStore;
import io.minio.*;
import io.minio.errors.ErrorResponseException;

import java.io.InputStream;
import java.util.Map;

/**
 * Minio文件存储插件
 */
public class MinioFileStore implements FileStore {

    private final MinioClient minioClient;
    private final String defaultBucketName;

    public MinioFileStore(MinioClient minioClient, String defaultBucketName) {
        this.minioClient = minioClient;
        this.defaultBucketName = defaultBucketName;
    }

    @Override
    public void save(String path, InputStream inputStream, Map<String, Object> runtimeParams) {
        try {
            // 检查bucket是否存在，如果不存在则创建
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(defaultBucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(defaultBucketName).build());
            }

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucketName)
                            .object(path)
                            // -1表示未知大小, 10MB part size
                            .stream(inputStream, -1, 10485760)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file to MinIO", e);
        }
    }

    @Override
    public InputStream get(String path, Map<String, Object> runtimeParams) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(defaultBucketName)
                            .object(path)
                            .build());
        } catch (Exception e) {
            // 如果文件不存在，MinIO会抛出异常，这里我们选择返回null，符合接口预期
            if (e instanceof ErrorResponseException && "NoSuchKey".equals(((ErrorResponseException) e).errorResponse().code())) {
                return null;
            }
            throw new RuntimeException("Failed to get file from MinIO", e);
        }
    }

    @Override
    public void remove(String path, Map<String, Object> runtimeParams) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucketName)
                            .object(path)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove file from MinIO", e);
        }
    }

    @Override
    public boolean exists(String path, Map<String, Object> runtimeParams) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(defaultBucketName)
                            .object(path)
                            .build());
            return true;
        } catch (Exception e) {
            // statObject 在文件不存在时会抛出异常
            if (e instanceof ErrorResponseException && "NoSuchKey".equals(((ErrorResponseException) e).errorResponse().code())) {
                return false;
            }
            // 其他异常则向上抛出
            throw new RuntimeException("Failed to check if file exists in MinIO", e);
        }
    }
}