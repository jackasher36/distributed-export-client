// src/main/java/com/jackasher/ageiport/listener/AttachmentSubTaskCompletionListener.java
package com.jackasher.ageiport.listener;

import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.constants.ExecuteType;
import com.alibaba.ageiport.processor.core.eventbus.local.async.Subscribe;
import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.alibaba.ageiport.processor.core.spi.listener.ManageableListener;
import com.alibaba.ageiport.processor.core.spi.task.monitor.TaskStageEvent;
import com.alibaba.ageiport.processor.core.spi.task.selector.TaskSpiSelector;
import com.alibaba.ageiport.processor.core.spi.task.stage.Stage;
import com.alibaba.ageiport.processor.core.spi.task.stage.SubTaskStageProvider;
import com.jackasher.ageiport.utils.AttachmentProcessUtil;

public class AttachmentSubTaskCompletionListener implements ManageableListener<TaskStageEvent> {

    private static final Logger log = LoggerFactory.getLogger(AttachmentSubTaskCompletionListener.class);

    private AgeiPort ageiPort;

    @Override
    public void startListen(AgeiPort ageiPort) {
        // 将 Spring 注入的 ageiPort 实例赋给成员变量，确保后续使用
        this.ageiPort = ageiPort;
        // 在本地和集群两种模式下都注册此监听器
        ageiPort.getEventBusManager().getEventBus(ExecuteType.STANDALONE).register(this);
        ageiPort.getEventBusManager().getEventBus(ExecuteType.CLUSTER).register(this);
        log.info("AttachmentSubTaskCompletionListener 已启动并注册。");
    }

    @Subscribe
    @Override
    public void handle(TaskStageEvent event) {
        // 1. 只处理子任务事件
        if (!event.isSubTaskEvent()) {
            return;
        }

        try {
            String mainTaskId = event.getMainTaskId();
            MainTask mainTask = ageiPort.getTaskServerClient().getMainTask(mainTaskId);

            // 健壮性检查：如果主任务信息获取失败，直接返回
            if (mainTask == null) {
                log.warn("无法获取到 MainTask 信息，mainTaskId: {}，跳过事件处理", mainTaskId);
                return;
            }


            // 2. 通过 SPI 选择器获取正确的 Stage Provider
            TaskSpiSelector spiSelector = ageiPort.getTaskSpiSelector();
            SubTaskStageProvider stageProvider = spiSelector.selectExtension(
                    mainTask.getExecuteType(),
                    mainTask.getType(),
                    mainTask.getCode(),
                    SubTaskStageProvider.class
            );

            // 健壮性检查：防止 stageProvider 为 null 导致空指针
            if (stageProvider == null) {
                log.error("无法找到匹配的 SubTaskStageProvider，任务信息: executeType={}, type={}, code={}",
                        mainTask.getExecuteType(), mainTask.getType(), mainTask.getCode());
                return;
            }

            // 3. 调用 public 方法获取 FINISHED stage
            Stage finishedStage = stageProvider.subTaskFinished();

            // 4. 判断是否是子任务完成事件
            if (finishedStage != null && finishedStage.getCode().equals(event.getStage())) {
                String subTaskId = event.getSubTaskId();
                log.info("监听到子任务 {} 完成事件，准备触发附件处理...", subTaskId);

                // 5. 调用您的核心业务逻辑
                AttachmentProcessUtil.triggerDeferredTasks(mainTask.getMainTaskId());
                log.info("已为任务 {} 触发延迟附件处理。", mainTask.getMainTaskId());

            }
        } catch (Exception e) {
            log.error("处理 TaskStageEvent 时发生未预期的异常, event: {}", event, e);
        }
    }
}