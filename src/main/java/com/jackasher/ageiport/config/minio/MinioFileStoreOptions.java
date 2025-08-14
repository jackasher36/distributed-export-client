// src/main/java/com/alibaba/ageiport/ext/file/store/minio/MinioFileStoreOptions.java
package com.jackasher.ageiport.config.minio;

import com.alibaba.ageiport.ext.file.store.FileStoreOptions;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MinioFileStoreOptions implements FileStoreOptions {

    public static final String TYPE = MinioConstants.TYPE;

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private MinioClient minioClient;

    @Override
    public String type() {
        return TYPE;
    }
}