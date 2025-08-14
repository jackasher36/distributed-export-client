// src/main/java/com/jackasher/ageiport/service/ExportConfigResolver.java
package com.jackasher.ageiport.utils.params.resolver;

import com.jackasher.ageiport.config.export.ExportProperties;
import com.jackasher.ageiport.model.export.ExportParams;
import com.jackasher.ageiport.utils.SpringContextUtil;
import org.springframework.stereotype.Service;

//@Service

/**
 * 暂废弃供参考,请使用reflect包下解析器
 */
@Deprecated
public class ExportConfigResolver {

    /**
     * 解析并合并API参数和配置文件，生成最终生效的配置对象。
     * @param apiParams 来自前端API请求的参数 (IrMessageQuery.getExportParams())
     * @return 一个包含了最终生效配置的不可变对象
     */
    public EffectiveExportConfig resolve(ExportParams apiParams) {
        // 1. 获取配置文件中的默认值
        ExportProperties defaultProps = SpringContextUtil.exportProperties();

        // 2. 使用ConfigUtils集中处理所有参数的选择逻辑
        int totalCount = ConfigUtils.choosePositive(apiParams.getTotalCount(), defaultProps.getTotalCount());
        int excelRowNumber = ConfigUtils.choosePositive(apiParams.getExcelRowNumber(), defaultProps.getExcelRowNumber());
        int sheetRowNumber = ConfigUtils.choosePositive(apiParams.getSheetRowNumber(), defaultProps.getSheetRowNumber());
        int pageRowNumber = ConfigUtils.choosePositive(apiParams.getPageRowNumber(), defaultProps.getPageRowNumber());

        boolean deleteTempFile = ConfigUtils.choose(apiParams.getDeleteTempFile(), defaultProps.isDeleteTempFile());
        boolean deleteFileAfterExport = ConfigUtils.choose(apiParams.getDeleteFileAfterExport(), defaultProps.isDeleteFileAfterExport());

        //  处理 isProcessAttachments 的 getter 兼容性
        // 因为 ExportProperties 中字段是 isProcessAttachments, Lombok 生成的 getter 是 isProcessAttachments()
        // 而 ExportParams 中字段是 processAttachments, Lombok 生成的 getter 也是 isProcessAttachments()
        boolean processAttachments = ConfigUtils.choose(apiParams.getProcessAttachments(), defaultProps.isProcessAttachments());
        String fileTempDirectory = ConfigUtils.chooseNonBlank(apiParams.getFileTempDirectory(), defaultProps.getFileTempDirectory());
        String defaultBucketName = ConfigUtils.chooseNonBlank(apiParams.getDefaultBucketName(), defaultProps.getDefaultBucketName());

        long taskTimeout = ConfigUtils.choosePositive(apiParams.getTaskTimeout(), defaultProps.getTaskTimeout());

        // excelFileDirectory 字段在 ExportParams 中没有，所以直接用默认值
        String excelFileDirectory = ConfigUtils.chooseNonBlank(apiParams.getExcelFileDirectory(), defaultProps.getExcelFileDirectory());

        String excelFileName = ConfigUtils.chooseNonBlank(apiParams.getExcelFileName(), defaultProps.getExcelFileName());

        // 4. 处理其他字段

        // 3. 创建并返回最终的配置对象
        return new EffectiveExportConfig(
                totalCount,
                excelRowNumber,
                sheetRowNumber,
                pageRowNumber,
                deleteTempFile,
                fileTempDirectory,
                taskTimeout,
                defaultBucketName,
                deleteFileAfterExport,
                processAttachments,
                excelFileDirectory,
                excelFileName
        );
    }
}