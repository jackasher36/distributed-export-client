package com.jackasher.ageiport.controller;

import com.alibaba.ageiport.common.feature.FeatureUtils;
import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.constants.MainTaskFeatureKeys;
import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.alibaba.ageiport.processor.core.spi.service.*;
import com.alibaba.fastjson.JSON;
import com.jackasher.ageiport.constant.TaskSpecificationCode;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * IR消息导出控制器
 *
 * @author Jackasher
 * @version 1.0
 * @className IrMessageExportController
 * @since 1.0
 **/
@RestController
@RequestMapping("/ir-message")
public class IrMessageExportController {

    static Logger logger = LoggerFactory.getLogger(IrMessageExportController.class);

    @Resource
    private AgeiPort ageiPort;

    @Resource
    private DiscoveryClient discoveryClient;

    @Value("${spring.application.name}")
    private String appName;

    @PostMapping("/export")
    public TaskExecuteResult exportIrMessages(@RequestBody IrMessageQuery irMessageQuery) {
        TaskExecuteParam request = new TaskExecuteParam();
        request.setTaskSpecificationCode(TaskSpecificationCode.IR_MESSAGE_EXPORT_PROCESSOR);
        request.setBizQuery(JSON.toJSONString(irMessageQuery));

        // ======== 扩展点：添加用户信息 ========
        // User currentUser = SecurityUtils.getCurrentUser();
        // request.setBizUserId(currentUser.getId());
        // request.setBizUserName(currentUser.getName());
        // request.setBizUserTenant(currentUser.getTenantId());

        int nodeCount = ageiPort.getClusterManager().getNodes().size();
        logger.info("IR消息导出 - 集群节点数量: {}", nodeCount);

        int size = discoveryClient.getServices().size();
        logger.info("服务发现 - 注册服务数量: {}", size);

        List<ServiceInstance> instances = discoveryClient.getInstances(appName);
        instances.forEach(instance -> logger.info("服务发现 - 实例: {}", instance));

        // 任务执行
        TaskService taskService = ageiPort.getTaskService();
        TaskExecuteResult result = taskService.executeTask(request);

        logger.info("IR消息导出任务已提交，任务ID: {}", result.getMainTaskId());
        return result;
    }

    // --- 新增的进度查询接口 ---
    @GetMapping("/export/progress/{mainTaskId}")
    public TaskProgressResult getExportProgress(@PathVariable("mainTaskId") String mainTaskId) {
        TaskService taskService = ageiPort.getTaskService();
        TaskProgressParam progressParam = new TaskProgressParam();
        progressParam.setMainTaskId(mainTaskId);
        TaskProgressResult progressResult = taskService.getTaskProgress(progressParam);

        // ======== 扩展点：任务完成后附带下载链接 ========
        if (progressResult.getIsFinished() != null && progressResult.getIsFinished()) {
            // 任务完成后，从数据库或其他地方获取完整的任务信息
            MainTask mainTask = ageiPort.getTaskServerClient().getMainTask(mainTaskId);
            String outputFileKey = FeatureUtils.getFeature(mainTask.getFeature(), MainTaskFeatureKeys.OUTPUT_FILE_KEY);

            if (outputFileKey != null) {
                // String downloadUrl = fileStoreService.generatePresignedUrl(outputFileKey, 3600);
                // 构造一个包含下载链接的、更丰富的响应对象
                // RichProgressResponse richResponse = new RichProgressResponse(progressResult, downloadUrl);
                // return ResponseEntity.ok(richResponse);
            }
        }
        //TODO=====================
        return null;
    }

    @GetMapping("/export/history")
    public String getTaskHistory(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        // String currentUserId = SecurityUtils.getCurrentUser().getId();
        String currentUserId = "user-001"; // 假设
        // 从自己的业务数据库中查询该用户提交的任务列表
        // return myBusinessTaskService.getHistoryTasks(currentUserId, page, size);
        //TODO
        return null; // 伪代码
    }

    @GetMapping("/ping")
    public String ping() {
        return "IR Message Export Service - " + System.currentTimeMillis();
    }
} 