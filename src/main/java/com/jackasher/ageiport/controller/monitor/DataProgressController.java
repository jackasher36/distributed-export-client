package com.jackasher.ageiport.controller.monitor;

import com.alibaba.ageiport.common.feature.FeatureUtils;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.constants.MainTaskFeatureKeys;
import com.alibaba.ageiport.processor.core.constants.TaskStatus;
import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.constant.PostProcessingTaskStatus;
import com.jackasher.ageiport.model.dto.FullProgress;
import com.jackasher.ageiport.service.data_processing_service.ProgressTrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 导出任务进度聚合查询控制器 (最终形态)
 *
 * @author Jackasher
 */
@RestController
@RequestMapping("/export")
public class DataProgressController {

    private static final Logger log = LoggerFactory.getLogger(DataProgressController.class);

    @Resource
    private ProgressTrackerService progressTracker;

    @Resource
    private AgeiPort ageiPort;

    /**
     * 获取完整的导出进度，包括核心数据导出和异步附件处理。
     *
     * @param mainTaskId 主任务ID
     * @return 包含聚合进度信息的 ResponseEntity
     */
    @GetMapping("/full-progress/{mainTaskId}")
    public ResponseEntity<Map<String, Object>> getFullExportProgress(@PathVariable("mainTaskId") String mainTaskId) {

        // 1. 直接从持久化存储中获取 MainTask 实体作为核心进度的真相来源
        MainTask mainTask = ageiPort.getTaskServerClient().getMainTask(mainTaskId);

        if (mainTask == null) {
            log.warn("任务不存在, MainTaskID: {}", mainTaskId);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Task not found.");
            errorResponse.put("mainTaskId", mainTaskId);
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }

        // 2. 构建核心导出进度的前端视图
        Map<String, Object> coreProgressView = buildCoreProgressView(mainTask);

        // 3. 获取附件处理进度
        FullProgress attachmentProgress = progressTracker.geFullProgress(mainTaskId);

        // 4. 构建最终的 API 响应 Map
        Map<String, Object> response = new HashMap<>();
        response.put("dataExportProgress", coreProgressView);
        response.put("attachmentProcessingProgress", attachmentProgress);

        // 5. 计算并添加总体进度和状态
        calculateAndSetOverallStatus(response, coreProgressView, attachmentProgress);

        // 6. 根据最终状态决定是否附加下载链接
        String overallStatus = (String) response.get("overallStatus");
        if ("COMPLETED".equals(overallStatus) || "PARTIALLY_COMPLETED".equals(overallStatus)) {
            response.put("downloadUrl", generateDownloadUrl(mainTask));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 根据 MainTask 实体构建一个与 TaskProgressResult 结构类似的前端视图。
     * 解决了内存中 TaskProgressResult 可能被清理的问题。
     *
     * @param mainTask 持久化的主任务实体
     * @return 一个Map，结构与 TaskProgressResult 类似
     */
    private Map<String, Object> buildCoreProgressView(MainTask mainTask) {
        Map<String, Object> view = new HashMap<>();
        view.put("mainTaskId", mainTask.getMainTaskId());
        view.put("status", mainTask.getStatus());

        boolean isFinished = TaskStatus.FINISHED.equals(mainTask.getStatus());
        boolean isError = TaskStatus.ERROR.equals(mainTask.getStatus());

        view.put("isFinished", isFinished);
        view.put("isError", isError);

        double percent;
        if (isFinished || isError) {
            percent = 100.0;
        } else {
            // 如果任务还在进行中，可以根据子任务完成比例估算一个进度
            Integer total = mainTask.getSubTotalCount();
            Integer finished = mainTask.getSubFinishedCount();
            if (total != null && total > 0 && finished != null) {
                percent = ((double) finished / total) * 100.0;
            } else {
                percent = 0.0; // 尚未开始或信息不足
            }
        }
        view.put("percent", percent);

        view.put("totalSubTaskCount", mainTask.getSubTotalCount());
        view.put("finishedSubTaskCount", mainTask.getSubFinishedCount());
        view.put("successSubTaskCount", mainTask.getSubSuccessCount());
        view.put("errorSubTaskCount", mainTask.getSubFailedCount());

        return view;
    }

    /**
     * 计算总体状态和百分比，以适应异步解耦的流程。
     */
    private void calculateAndSetOverallStatus(Map<String, Object> responseMap, Map<String, Object> coreProgressView, FullProgress attachmentProgress) {
        String coreStatus = getCoreStatus(coreProgressView);
        String attachmentStatus = getAttachmentStatus(attachmentProgress);

        String overallStatus;
        if ("FAILED".equals(coreStatus) || "FAILED".equals(attachmentStatus)) {
            overallStatus = "FAILED";
        } else if (isFinalStatus(coreStatus) && isFinalStatus(attachmentStatus)) {
            if ("PARTIALLY_COMPLETED".equals(attachmentStatus)) {
                overallStatus = "PARTIALLY_COMPLETED";
            } else {
                overallStatus = "COMPLETED";
            }
        } else {
            overallStatus = "PROCESSING";
        }
        responseMap.put("overallStatus", overallStatus);

        double corePercent = (double) coreProgressView.getOrDefault("percent", 0.0);
        double attachmentPercent = (attachmentProgress != null && attachmentProgress.getSummary() != null)
                ? attachmentProgress.getSummary().getPercent() : 0.0;

        double overallPercent;
        if (isFinalStatus(coreStatus)) {
            // 核心导出已完成，总进度 = 40% (基础) + 附件进度的60%
            overallPercent = 40.0 + (attachmentPercent * 0.6);
        } else {
            // 核心导出未完成，总进度主要由核心导出决定，不能超过40%
            overallPercent = corePercent * 0.4;
        }
        responseMap.put("overallPercent", Math.min(overallPercent, 100.0));
    }

    /**
     * 将核心进度的视图 Map 转换为统一的状态字符串。
     */
    private String getCoreStatus(Map<String, Object> coreProgressView) {
        String status = (String) coreProgressView.getOrDefault("status", TaskStatus.NEW);
        if (TaskStatus.ERROR.equals(status)) {
            return "FAILED";
        }
        if (TaskStatus.FINISHED.equals(status)) {
            return "COMPLETED";
        }
        return "PROCESSING";
    }

    /**
     * 安全地从 FullProgress DTO 获取附件处理的状态。
     */
    private String getAttachmentStatus(FullProgress attachmentProgress) {
        if (attachmentProgress == null || attachmentProgress.getSummary() == null) {
            return "PENDING"; // 附件进度还未初始化
        }
        return attachmentProgress.getSummary().getStatus();
    }

    /**
     * 判断一个状态是否是最终状态 (已结束)。
     */
    private boolean isFinalStatus(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status)
                || "PARTIALLY_COMPLETED".equals(status) || "SKIPPED".equals(status);
    }

    /**
     * 根据 MainTask 对象生成文件的下载链接。
     */
    private String generateDownloadUrl(MainTask mainTask) {
        try {
            if (mainTask == null) return null;

            String outputFileKey = FeatureUtils.getFeature(mainTask.getFeature(), MainTaskFeatureKeys.OUTPUT_FILE_KEY);

            if (outputFileKey != null && !outputFileKey.isEmpty()) {
                // TODO: 替换为你的真实文件下载服务的URL
                return "/api/files/download?fileKey=" + outputFileKey;
            } else {
                log.info("任务 {} 已完成，但没有输出文件 (outputFileKey is null)", mainTask.getMainTaskId());
                return null;
            }
        } catch (Exception e) {
            log.error("为任务 {} 生成下载链接时发生异常", mainTask.getMainTaskId(), e);
            return null;
        }
    }
}