package com.jackasher.ageiport.utils.params.resolver;// src/main/java/com/jackasher/ageiport/model/export/EffectiveExportConfig.java

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * 最终生效的导出配置。
 * 这是一个不可变的DTO，由 ExportConfigResolver 创建。
 *
 */
@Getter
@ToString
@AllArgsConstructor
@Deprecated
public class EffectiveExportConfig {

    private final int totalCount;
    private final int excelRowNumber;
    private final int sheetRowNumber;
    private final int pageRowNumber;
    private final boolean deleteTempFile;
    private final String fileTempDirectory;
    private final long taskTimeout;
    private final String defaultBucketName;
    private final boolean deleteFileAfterExport;
    private final boolean processAttachments;
    private final String excelFileDirectory;
    private final String excelFileName;

}