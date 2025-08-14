package com.jackasher.ageiport.model.download;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DownloadFileParam {

    /**
     * 通讯类型,c:通话;l:IP数据;s:短信;t:位置更新;b:SBD;d:SBD1,SBD2
     */
    private String communicateType;

    /**
     * Minio 桶名称
     */
    private String bucketName;

    /**
     * 压缩包名称
     */
    private String archiveName;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 保存到zip中的文件重命名, 避免重名文件
     */
    private String writeZipFileName;

    /**
     * 保存到zip的文件名称 保存zip中的文件重命名, 避免重名文件
     */
    private String writeZipBeforeDecodeFileName;

    public DownloadFileParam(String communicateType, String bucketName, String archiveName, String fileName) {
        this.communicateType = communicateType;
        this.bucketName = bucketName;
        this.archiveName = archiveName;
        this.fileName = fileName;
        // 默认认为写入zip的文件名和原始文件名相同
        this.writeZipFileName = fileName;
    }
}