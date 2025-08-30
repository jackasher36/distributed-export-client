package com.jackasher.ageiport.service.data_processing_service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.ageiport.common.utils.TaskIdUtil;
import com.jackasher.ageiport.annotation.Timing;
import com.jackasher.ageiport.constant.PostProcessingTaskStatus;
import com.jackasher.ageiport.demo.DownloadFileParamDemo;
import com.jackasher.ageiport.model.export.FilePaths;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.service.data_processing_service.AbstractDataProcessingServiceAdapter;
import com.jackasher.ageiport.service.monitor.ProgressTrackerService;
import static com.jackasher.ageiport.utils.business.IrMessageUtils.buildFilePaths;
import static com.jackasher.ageiport.utils.business.IrMessageUtils.getResolvedParams;

@Service
public class AttachmentProcessingServiceImpl extends AbstractDataProcessingServiceAdapter<IrMessageData, IrMessageQuery> {

    private static final Logger log = LoggerFactory.getLogger(AttachmentProcessingServiceImpl.class);

    @Resource
    ProgressTrackerService progressTracker;

    @Override
    @Timing(value = "附件批量处理", unit = "s")
    protected void doProcessData(List<IrMessageData> messages, String subTaskId, int pageNum, IrMessageQuery irMessageQuery) throws Exception {
        String mainTaskId = TaskIdUtil.getMainTaskId(subTaskId);
        int totalItemsInThisBatch = (messages != null) ? messages.size() : 0;

        // 在处理开始时，更新本批次的附件总数
        progressTracker.updateTotalItemsForSubTask(mainTaskId, subTaskId, totalItemsInThisBatch);
        log.info("正式开始处理附件批次, MainTaskID: {}, SubTaskID: {}, 附件总数: {}", mainTaskId, subTaskId, totalItemsInThisBatch);

        // 如果没有附件，直接标记完成并返回
        if (totalItemsInThisBatch == 0) {
            progressTracker.markSubTaskAsFinished(mainTaskId, subTaskId, PostProcessingTaskStatus.COMPLETED, "无附件需要处理");
            return;
        }

        // 构建文件路径
        FilePaths filePaths = buildFilePaths(subTaskId, irMessageQuery);
        log.info("生成的文件路径: Excel文件名={}, Excel目录={}, 处理后ZIP={}, 解码前ZIP={}", 
                filePaths.excelFileName, filePaths.excelDirectory, filePaths.outZipFileName, filePaths.beforeDecodeZipFileName);

        // 将 IrMessageData 列表转换为 DownloadFileParam 列表
        log.info("开始将 IrMessageData 列表转换为 DownloadFileParam 列表...");
        List<DownloadFileParamDemo> downloadParams = messages.stream()
                .map(msg -> {
                    DownloadFileParamDemo param = this.createDownloadParam(msg);
                    if (param == null) {
                        log.trace("IrMessageData (UUID: {}) 未能生成有效的下载参数，因为 diePickingFileName 为空或空字符串。", msg.getUuid());
                    } else {
                        log.trace("IrMessageData (UUID: {}) 成功生成下载参数: {}", msg.getUuid(), param);
                    }
                    return param;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        log.info("成功生成 {} 个有效的 DownloadFileParam。", downloadParams.size());

        if (downloadParams.isEmpty()) {
            log.warn("子任务 {} 的批次 {} 未生成任何有效的处理参数，跳过附件下载。", subTaskId, pageNum);
            return;
        }

        // 权限校验 (模拟)
        log.info("  [模拟] 权限校验通过...");

        log.info("开始调用 MinioDownloadHelperDemo 下载并压缩文件...");
        log.info("下载目录: {}", filePaths.excelDirectory);
        log.info("处理后 Zip 文件名: {}", filePaths.outZipFileName);
        log.info("解码前 Zip 文件名: {}", filePaths.beforeDecodeZipFileName);
        log.info("下载参数列表大小: {}", downloadParams.size());


        Boolean processAttachments = getResolvedParams(irMessageQuery).getProcessAttachments();
        if (processAttachments) {
            log.info("开始处理附件...");
            //模拟事件处理中
            //MinioUtils.downloadFileInCompressFile(minioClient, excelDirectory, outZipFileName, beforeDecodeZipFileName, downloadParams, true, true);
            log.info("生成目录: {}", filePaths.excelDirectory);
            Thread.sleep(5_000);
        } else {
            log.info("附件处理被禁用，跳过附件处理。");
        }

        // 标记处理完成
        int totalFailed = (int) progressTracker.getSubTaskDetail(mainTaskId, subTaskId).get().getFailedItems();
        PostProcessingTaskStatus finalStatus = (totalFailed > 0) ?
                PostProcessingTaskStatus.PARTIALLY_COMPLETED :
                PostProcessingTaskStatus.COMPLETED;
        String resultMessage = String.format("处理完成。成功: %d, 失败: %d", downloadParams.size() - totalFailed, totalFailed);
        progressTracker.markSubTaskAsFinished(mainTaskId, subTaskId, finalStatus, resultMessage);

        log.info("附件批次处理完成, SubTaskID: {}. {}", subTaskId, resultMessage);
        log.info("子任务 {} 的批次 {} 附件处理并打包成功。", subTaskId, pageNum);
    }

    private DownloadFileParamDemo createDownloadParam(IrMessageData msg) {
        log.trace("尝试为 IrMessageData (UUID: {}) 创建下载参数。", msg.getUuid());
        // 假设需要处理的文件是 diePickingFileName
        if (msg.getDiePickingFileName() != null && !msg.getDiePickingFileName().isEmpty()) {
            DownloadFileParamDemo param = new DownloadFileParamDemo(
                    msg.getDataSourceType(),
                    msg.getBucketName(),
                    msg.getArchiveName(),
                    msg.getDiePickingFileName()
            );
            param.setWriteZipFileName(msg.getUuid() + "_" + msg.getDiePickingFileName());
            param.setWriteZipBeforeDecodeFileName("before_decode_" + param.getWriteZipFileName());
            log.trace("成功为 IrMessageData (UUID: {}) 创建下载参数: {}", msg.getUuid(), param);
            return param;
        }
        log.info("IrMessageData (UUID: {}) 的 diePickingFileName 为空或空字符串，未创建下载参数。", msg.getUuid());
        return null;
    }


}
