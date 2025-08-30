package com.jackasher.ageiport.listener;

import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.constants.ExecuteType;
import com.alibaba.ageiport.processor.core.eventbus.local.async.Subscribe;
import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.alibaba.ageiport.processor.core.spi.listener.ManageableListener;
import com.alibaba.ageiport.processor.core.spi.task.monitor.TaskStageEvent;
import com.alibaba.ageiport.processor.core.task.event.WaitDispatchSubTaskEvent;
import com.jackasher.ageiport.constant.PostProcessingTaskStatus;
import com.jackasher.ageiport.model.export.ExportParams;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.service.monitor.ProgressTrackerService;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;
import com.jackasher.ageiport.utils.params.reflect.ExportConfigResolver;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

/**
 * 附件处理进度初始化监听器。
 * 监听AGEIPort的子任务分发事件，在此刻进行附件处理进度的宏观初始化。
 * 【最终简化版】：直接使用 SpringContextUtil 获取所需 Bean，无需代理。
 */
@Component
public class ProgressInitializerListener implements ManageableListener<WaitDispatchSubTaskEvent> {

    private static final Logger log = LoggerFactory.getLogger(ProgressInitializerListener.class);
    
    // 这个 ageiPort 实例是由框架在 startListen 时传入的，是有效的。
    private AgeiPort ageiPort;

    @Override
    public void startListen(AgeiPort ageiPort) {
        this.ageiPort = ageiPort;
        // 只监听本地事件总线，因为这个事件是在主任务节点上发布的
        ageiPort.getEventBusManager().getEventBus(ExecuteType.STANDALONE).register(this);
        log.info("ProgressInitializerListener 已启动并注册。");
    }

    @Subscribe
    @Override
    public void handle(WaitDispatchSubTaskEvent event) {
        String mainTaskId = event.getMainTaskId();
        try {
            // ==================== 核心改动 ====================
            // 在需要时，直接从 Spring 上下文中获取 Bean 实例
            ProgressTrackerService progressTracker = SpringContextUtil.getBean(ProgressTrackerService.class);
            ExportConfigResolver exportConfigResolver = SpringContextUtil.getBean(ExportConfigResolver.class);
            // ===============================================

            MainTask mainTask = ageiPort.getTaskServerClient().getMainTask(mainTaskId);
            if (mainTask == null) {
                log.warn("无法找到主任务 {}，跳过进度初始化。", mainTaskId);
                return;
            }

            // 1. 判断该任务是否需要处理附件
            IrMessageQuery query = JSON.parseObject(mainTask.getBizQuery(), IrMessageQuery.class);
            ExportParams effectiveParams = exportConfigResolver.resolve(query.getExportParams());

            if (!Boolean.TRUE.equals(effectiveParams.getProcessAttachments())) {
                log.info("任务 {} 被配置为不处理附件，跳过进度初始化。", mainTaskId);
                return;
            }
            
            // 2. 获取子任务总数
            Integer totalSubTasks = mainTask.getSubTotalCount();
            if (totalSubTasks == null || totalSubTasks <= 0) {
                log.warn("任务 {} 没有子任务，跳过附件进度初始化。", mainTaskId);
                // 即使没有子任务，也初始化一个“已完成”的进度
                progressTracker.initializeSummary(mainTaskId, 0);
                progressTracker.markSubTaskAsFinished(mainTaskId, "subTaskId", PostProcessingTaskStatus.COMPLETED, "无子任务");
                return;
            }

            // 3. 执行宏观进度初始化
            progressTracker.initializeSummary(mainTaskId, totalSubTasks);
            log.info("附件处理宏观进度已初始化, MainTaskID: {}, 总批次数: {}", mainTaskId, totalSubTasks);

        } catch (Exception e) {
            log.error("初始化附件处理宏观进度时发生异常, MainTaskID: {}", mainTaskId, e);
        }
    }
}