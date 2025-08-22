// src/main/java/com/jackasher/ageiport/callback/MainTaskCallback.java
package com.jackasher.ageiport.callback;

import java.util.HashMap;

import javax.annotation.Resource;

import com.jackasher.ageiport.publisher.DeferredTaskTriggerService;
import com.jackasher.ageiport.utils.business.AttachmentProcessUtil;
import org.springframework.stereotype.Component;

import com.alibaba.ageiport.common.feature.FeatureUtils;
import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;
import com.alibaba.ageiport.common.utils.TaskIdUtil;
import com.alibaba.ageiport.ext.file.store.FileStore;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.constants.MainTaskFeatureKeys;
import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.constant.MainTaskCallbackConstant;
import com.jackasher.ageiport.service.callback_service.AlertService;
import com.jackasher.ageiport.service.callback_service.BusinessTaskService;
import com.jackasher.ageiport.service.callback_service.WebSocketService;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;

/**
 * 任务回调钩子,全局只执行一次。
 * 这个回调在所有任务（无论成功、失败）的生命周期关键点被触发，是处理任务后业务逻辑的中心枢纽。
 * 通过 @Component 注解使其成为一个 Spring Bean，以便注入其他服务。
 */
@Component(MainTaskCallbackConstant.MAIN_TASK_CALLBACK_BEAN_NAME)
public class MainTaskCallback implements com.alibaba.ageiport.processor.core.spi.task.callback.MainTaskCallback {

    private static final Logger logger = LoggerFactory.getLogger(MainTaskCallback.class);

    @Resource
    private BusinessTaskService businessTaskService;

    @Resource
    private WebSocketService webSocketService;

    @Resource
    private AlertService alertService;

    @Resource
    private DeferredTaskTriggerService deferredTaskTriggerService;


    @Override
    public void afterCreated(MainTask mainTask) {
        logger.info("--- [CALLBACK] 任务创建后 afterCreated: {}", mainTask.getMainTaskId());
        try {
            if (businessTaskService == null || webSocketService == null) {
                logger.error("依赖注入失败！BusinessTaskService 或 WebSocketService 为 null。请检查Spring配置。");
                return;
            }
            businessTaskService.createTaskRecord(mainTask.getBizKey(), mainTask.getMainTaskId(), mainTask.getBizUserId(), "排队中");
            webSocketService.sendProgressUpdate(mainTask.getBizUserId(), "任务已创建，正在排队...", 1.0, mainTask.getMainTaskId());
            logger.info("用户 {} 创建了导出任务 {}，业务标识为 {}", mainTask.getBizUserId(), mainTask.getName(), mainTask.getBizKey());
        } catch (Exception e) {
            logger.error("在 afterCreated 回调中处理业务逻辑时发生异常, TaskId: {}", mainTask.getMainTaskId(), e);
        }
    }

    @Override
    public void beforeFinished(MainTask mainTask) {
        logger.info("--- [CALLBACK] 任务完成前 beforeFinished: {}", mainTask.getMainTaskId());
    }

    @Override
    public void afterFinished(MainTask mainTask) {
        logger.info("--- [CALLBACK] 任务成功完成 afterFinished: {}", mainTask.getMainTaskId());
        try {
            // 触发延迟处理的附件任务
            AttachmentProcessUtil.triggerDeferredTasks(mainTask.getMainTaskId());
            
            String feature = mainTask.getFeature();
            String outputFileKey = FeatureUtils.getFeature(feature, MainTaskFeatureKeys.OUTPUT_FILE_KEY);

            if (outputFileKey == null) {
                logger.warn("任务 {} 成功完成，但没有找到输出文件Key (outputFileKey)。", mainTask.getMainTaskId());
                businessTaskService.updateTaskSuccess(mainTask.getBizKey(), "任务成功，无文件产出", null);
                webSocketService.sendCompletionMessage(mainTask.getBizUserId(), "任务处理完成！", mainTask.getMainTaskId(), null);
                return;
            }

            logger.info("任务 {} 的导出文件已生成，存储Key: {}", mainTask.getMainTaskId(), outputFileKey);
            String downloadUrl = "/api/files/download?fileKey=" + outputFileKey;
            businessTaskService.updateTaskSuccess(mainTask.getBizKey(), "导出成功", downloadUrl);
            webSocketService.sendCompletionMessage(mainTask.getBizUserId(), "您的报表已生成，请点击下载。", mainTask.getMainTaskId(), downloadUrl);
            deferredTaskTriggerService.trigger(mainTask);
        } catch (Exception e) {
            logger.error("在 afterFinished 回调中处理业务逻辑时发生异常, TaskId: {}", mainTask.getMainTaskId(), e);
        }
    }

    @Override
    public void beforeError(MainTask mainTask) {
        logger.error("--- [CALLBACK] 任务失败前 beforeError: {}, 即将开始处理失败逻辑...", mainTask.getMainTaskId());
    }

    @Override
    public void afterError(MainTask mainTask) {
        logger.error("--- [CALLBACK] 任务失败后 afterError: {}, 错误信息: {}", mainTask.getMainTaskId(), mainTask.getResultMessage());
        try {
            businessTaskService.updateTaskFailure(mainTask.getBizKey(), mainTask.getResultMessage());
            webSocketService.sendFailureMessage(mainTask.getBizUserId(), "抱歉，任务处理失败: " + mainTask.getResultMessage(), mainTask.getMainTaskId());
            alertService.sendAlert("关键导出任务失败",
                    String.format("任务ID: %s\n业务Key: %s\n错误信息: %s",
                            mainTask.getMainTaskId(), mainTask.getBizKey(), mainTask.getResultMessage()));
            cleanupStorageFiles(mainTask);
        } catch (Exception e) {
            logger.error("在 afterError 回调中处理业务逻辑时发生异常, TaskId: {}", mainTask.getMainTaskId(), e);
        }
    }

    private void cleanupStorageFiles(MainTask mainTask) {
        logger.info("开始清理任务 {} 在文件存储中的残留文件...", mainTask.getMainTaskId());
        try {
            // 在需要时，通过 SpringContextUtil.getBean() 延迟获取 AgeiPort 实例
            AgeiPort ageiPort = SpringContextUtil.getBean(AgeiPort.class);
            FileStore fileStore = ageiPort.getFileStore();

            Integer subTotalCount = mainTask.getSubTotalCount();
            if (subTotalCount != null && subTotalCount > 0) {
                logger.info("任务 {} 共有 {} 个子任务，开始逐一清理其中间文件...", mainTask.getMainTaskId(), subTotalCount);
                for (int i = 1; i <= subTotalCount; i++) {
                    String subTaskId = TaskIdUtil.genSubTaskId(mainTask.getMainTaskId(), i);
                    try {
                        fileStore.remove(subTaskId, new HashMap<>());
                        logger.debug("成功删除中间文件: {}", subTaskId);
                    } catch (Exception e) {
                        logger.warn("尝试删除中间文件 {} 时出现异常（可能是文件不存在），不影响流程: {}", subTaskId, e.getMessage());
                    }
                }
                logger.info("任务 {} 的子任务中间文件清理完成。", mainTask.getMainTaskId());
            } else {
                logger.info("任务 {} 没有子任务或子任务数量未知，无需清理中间文件。", mainTask.getMainTaskId());
            }

            String outputFileKey = FeatureUtils.getFeature(mainTask.getFeature(), MainTaskFeatureKeys.OUTPUT_FILE_KEY);
            if (outputFileKey != null && !outputFileKey.isEmpty()) {
                logger.info("任务 {} 存在最终文件记录，尝试清理: {}", mainTask.getMainTaskId(), outputFileKey);
                try {
                    fileStore.remove(outputFileKey, new HashMap<>());
                    logger.info("成功删除不完整的最终文件: {}", outputFileKey);
                } catch (Exception e) {
                    logger.warn("删除最终文件 {} 失败，可能文件未生成。错误: {}", outputFileKey, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("在清理文件存储中的文件时发生严重错误，任务ID: {}", mainTask.getMainTaskId(), e);
        }
    }
}