// src/main/java/com/jackasher/ageiport/listener/MainTaskCompletionListener.java
package com.jackasher.ageiport.listener;

import org.springframework.stereotype.Component;

import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.constants.ExecuteType;
import com.alibaba.ageiport.processor.core.eventbus.local.async.Subscribe;
import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.alibaba.ageiport.processor.core.spi.listener.ManageableListener;
import com.alibaba.ageiport.processor.core.spi.task.monitor.TaskStageEvent;
import com.alibaba.ageiport.processor.core.spi.task.selector.TaskSpiSelector;
import com.alibaba.ageiport.processor.core.spi.task.stage.MainTaskStageProvider;
import com.alibaba.ageiport.processor.core.spi.task.stage.Stage;

/**
 * TaskStageEvent事件监听器,只有主节点可以监听到
 */
@Component("mainTaskCompletionListener") // 注册为 Spring Bean
public class MainTaskCompletionListener implements ManageableListener<TaskStageEvent> {

    private static final Logger log = LoggerFactory.getLogger(MainTaskCompletionListener.class);

    private AgeiPort ageiPort;

    @Override
    public void startListen(AgeiPort ageiPort) {
        this.ageiPort = ageiPort;
        // 关键：监听 CLUSTER 事件总线，以接收来自其他节点的广播
        ageiPort.getEventBusManager().getEventBus(ExecuteType.CLUSTER).register(this);
        // 同时监听 STANDALONE，以兼容单机模式
        ageiPort.getEventBusManager().getEventBus(ExecuteType.STANDALONE).register(this);
        log.info("MainTaskCompletionListener 已启动并注册，监听主任务完成事件。");
    }

    @Subscribe
    @Override
    public void handle(TaskStageEvent event) {
        log.info("任务监听到事件: {}", event.getStage());
        // 步骤1: 只关心主任务事件
        if (event.isSubTaskEvent()) {
            return;
        }


        try {
            String mainTaskId = event.getMainTaskId();
            MainTask mainTask = ageiPort.getTaskServerClient().getMainTask(mainTaskId);
            if (mainTask == null) {
                log.warn("无法获取到 MainTask 信息，mainTaskId: {}，跳过事件处理", mainTaskId);
                return;
            }

            TaskSpiSelector spiSelector = ageiPort.getTaskSpiSelector();
            MainTaskStageProvider stageProvider = spiSelector.selectExtension(
                    mainTask.getExecuteType(), mainTask.getType(), mainTask.getCode(), MainTaskStageProvider.class);

            if (stageProvider == null) {
                log.error("无法找到匹配的 MainTaskStageProvider for task: {}", mainTask.getCode());
                return;
            }

            Stage finishedStage = stageProvider.mainTaskFinished();

            // 步骤2: 精确判断是否是主任务“完成”事件
            if (finishedStage != null && finishedStage.getCode().equals(event.getStage())) {
                log.info("监听到主任务 {} 完成事件！准备触发本节点上的延迟任务...", mainTaskId);

                // 步骤3: 触发本节点上存储的延迟任务
                // GenericProcessingDispatcher.deferredTasks 是静态的，所以它只属于当前节点的JVM
//                GenericProcessingDispatcher.triggerDeferredTasks(mainTaskId);
            }

        } catch (Exception e) {
            log.error("处理主任务完成事件时发生异常, event: {}", event, e);
        }
    }
}