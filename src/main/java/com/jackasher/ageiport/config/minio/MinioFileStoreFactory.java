package com.jackasher.ageiport.config.minio;

import com.alibaba.ageiport.ext.file.store.FileStore;
import com.alibaba.ageiport.ext.file.store.FileStoreFactory;
import com.alibaba.ageiport.ext.file.store.FileStoreOptions;
import com.jackasher.ageiport.utils.SpringContextUtil;
import io.minio.MinioClient;

public class MinioFileStoreFactory implements FileStoreFactory {
    @Override
    public FileStore create(FileStoreOptions fileStoreOptions) {
        MinioFileStoreOptions options = (MinioFileStoreOptions) fileStoreOptions;

        MinioClient minioClient = SpringContextUtil.minioClient();

        return new MinioFileStore(minioClient, options.getBucketName());
    }
}