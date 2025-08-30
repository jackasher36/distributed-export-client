package com.jackasher.ageiport.dispatcher;

import com.jackasher.ageiport.constant.BatchDataProcessMode;
import com.jackasher.ageiport.model.dto.ProcessContext;
import com.jackasher.ageiport.model.export.GenericExportQuery;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.mq.kafka.KafkaProducerService;
import com.jackasher.ageiport.mq.rabbitmq.MqProducerService;
import com.jackasher.ageiport.service.data_processing_service.GenericDataProcessingService;
import com.jackasher.ageiport.service.monitor.ProgressTrackerService;
import com.jackasher.ageiport.utils.business.IrMessageUtils;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Jackasher
 * 抽象为通用分发器,支持任意数据类型
 * @version 1.0
 * @className GenericProcessingDispatcher
 * @since 1.0
 **/
public class GenericProcessingDispatcher<DATA, QUERY extends GenericExportQuery> {

    private static final Logger log = LoggerFactory.getLogger(GenericProcessingDispatcher.class);

    // 存储延迟处理的任务 - 一个主任务对应多个子任务
    private static final Map<String, List<Runnable>> deferredTasks = new ConcurrentHashMap<>();

    // 处理器映射 - 每种模式对应一个处理器（优雅替代switch）
    private final Map<BatchDataProcessMode, ProcessHandler<DATA, QUERY>> handlers = new EnumMap<>(BatchDataProcessMode.class);

    private final GenericDataProcessingService<DATA, QUERY> service;

    public GenericProcessingDispatcher(GenericDataProcessingService<DATA, QUERY> service) {
        this.service = service;
    }

    {
        // 初始化处理器映射
        handlers.put(BatchDataProcessMode.SYNC, this::processSyncMode);
        handlers.put(BatchDataProcessMode.ASYNC, this::processAsyncMode);
        handlers.put(BatchDataProcessMode.DEFERRED, this::processDeferredMode);
        handlers.put(BatchDataProcessMode.NONE, this::processNoneMode);
        handlers.put(BatchDataProcessMode.RABBITMQ, this::processMqMode);
        handlers.put(BatchDataProcessMode.KAFKA, this::processKafkaMode);
    }


    @FunctionalInterface
    private interface ProcessHandler<DATA, QUERY extends GenericExportQuery> {
        void handle(ProcessContext<DATA, QUERY> context);
    }

    /**
     * 通用批处理方法 - 支持任意数据类型
     *
     * @param dataList   数据列表
     * @param subTaskId  子任务ID
     * @param subTaskNo  子任务编号
     * @param query      查询参数
     * @param mainTaskId 主任务ID
     */
    public void processBatchData(
            List<DATA> dataList, String subTaskId, int subTaskNo, QUERY query, String mainTaskId) {

        // ==================== 数据处理进度器初始化 ====================
        try {
            ProgressTrackerService progressTracker = SpringContextUtil.getBean(ProgressTrackerService.class);
            // 初始化这个子任务（批次）的进度
            progressTracker.initializeSubTask(mainTaskId, subTaskId, subTaskNo, dataList.size());
            log.info("初始化该批次子任务监控, MainTaskID: {}, SubTaskID: {}, 数据数量: {}", mainTaskId, subTaskId, dataList.size());
        } catch (Exception e) {
            log.error("初始化数据处理进度失败, MainTaskID: {}, SubTaskID: {}", mainTaskId, subTaskId, e);
        }
        // =======================================================
        BatchDataProcessMode batchDataProcessMode = BatchDataProcessMode.ASYNC; // 默认值

        batchDataProcessMode = IrMessageUtils.getResolvedParams(query).getBatchDataProcessMode();

        log.info("子任务 {} 使用批处理模式: {}", subTaskId, batchDataProcessMode);

        // 使用映射表 + 函数式编程替代switch
        ProcessContext<DATA, QUERY> context = new ProcessContext<>(dataList, subTaskId, subTaskNo, query, mainTaskId);
        handlers.getOrDefault(batchDataProcessMode, this::processNoneMode).handle(context);
    }


    // ==================== 延迟队列的处理策略 ====================

    /**
     * 主任务完成后触发延迟处理
     */
    public static void triggerDeferredTasks(String mainTaskId) {
        List<Runnable> tasks = deferredTasks.remove(mainTaskId);
        if (tasks != null && !tasks.isEmpty()) {
            log.info("主任务 {} 完成，开始执行 {} 个延迟的附件处理任务", mainTaskId, tasks.size());

            // 并行执行所有延迟任务
            tasks.forEach(CompletableFuture::runAsync);
        } else {
            log.info("主任务 {} 没有延迟的附件处理任务", mainTaskId);
        }
    }

    /**
     * 【新方法】以严格串行且失败即停止的方式触发延迟任务。
     *
     * @param mainTaskId      主任务ID
     * @param executorService 必须是单线程的 ExecutorService
     */
    public static void triggerDeferredTasksSerially(String mainTaskId, ExecutorService executorService) {
        List<Runnable> tasks = deferredTasks.remove(mainTaskId);
        if (tasks == null || tasks.isEmpty()) {
            log.info("主任务 {} 在本节点上没有需要延迟处理的附件任务", mainTaskId);
            return;
        }

        log.info("主任务 {} 完成，准备以【严格串行】方式执行 {} 个附件任务", mainTaskId, tasks.size());

        // 使用迭代器来手动控制流程
        final Iterator<Runnable> iterator = tasks.iterator();

        // 提交一个“引导”任务，它负责启动整个链式调用
        executorService.submit(() -> processNextTask(iterator, executorService, mainTaskId));
    }

    private static void processNextTask(Iterator<Runnable> iterator, ExecutorService executorService, String mainTaskId) {
        if (!iterator.hasNext()) {
            log.info("主任务 {} 的所有附件任务已全部执行完毕。", mainTaskId);
            return;
        }

        Runnable nextTask = iterator.next();
        Future<?> future = executorService.submit(nextTask);

        try {
            // 阻塞等待当前任务完成
            future.get();
            log.info("一个附件任务成功完成，准备执行下一个...");

            // 当前任务成功后，递归地提交下一个任务
            processNextTask(iterator, executorService, mainTaskId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
            log.error("附件处理链中有一个任务被中断，后续所有任务将不再执行。MainTaskID: {}", mainTaskId, e);
        } catch (Exception e) {
            // ExecutionException, CancellationException 等都会被捕获
            log.error("附件处理链中有一个任务失败，后续所有任务将不再执行。MainTaskID: {}, 失败原因: {}",
                    mainTaskId, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            // 出现异常，递归链中断，不再提交新任务。
        }
    }


    // ==================== 具体处理策略实现 ====================

    private void processSyncMode(ProcessContext<DATA, QUERY> ctx) {
        try {
            log.info("【同步模式】开始处理子任务 {} 的数据", ctx.subTaskId);

            if (!ctx.messages.isEmpty()) {
                service.processData(ctx.messages, ctx.subTaskId, ctx.subTaskNo, ctx.query);
            } else {
                log.warn("【同步模式】不支持的数据类型，跳过处理: {}", ctx.messages.getClass().getSimpleName());
            }

            log.info("【同步模式】子任务 {} 的数据处理完成", ctx.subTaskId);
        } catch (Exception e) {
            log.error("【同步模式】子任务 {} 的数据处理失败: {}", ctx.subTaskId, e.getMessage(), e);
        }
    }

    /**
     * MQ模式的处理方法 - 保留原有逻辑
     */
    private void processMqMode(ProcessContext<DATA, QUERY> ctx) {
        try {
            log.info("【MQ模式】开始处理子任务 {} 的数据（发送到消息队列）", ctx.subTaskId);

            // 1. 获取MQ生产者服务
            MqProducerService producerService = SpringContextUtil.getBean(MqProducerService.class);

            // 2. 直接发送 ProcessContext，不需要额外创建消息体
            producerService.sendAttachmentTask(ctx);

            log.info("【MQ模式】成功提交子任务 {} 的附件处理消息到RabbitMQ", ctx.subTaskId);

        } catch (Exception e) {
            log.error("【MQ模式】处理子任务 {} 的数据失败（发送消息时异常）: {}", ctx.subTaskId, e.getMessage(), e);
            // 降级逻辑
            handleFailure(ctx, e);
        }
    }

    /**
     * Kafka模式的处理方法 - 保留原有逻辑
     */
    private void processKafkaMode(ProcessContext<DATA, QUERY> ctx) {
        try {
            log.info("【Kafka模式】开始处理子任务 {} 的数据（发送到Kafka）", ctx.subTaskId);

            // 1. 获取Kafka生产者服务
            KafkaProducerService producerService =
                    SpringContextUtil.getBean(KafkaProducerService.class);

            // 2. 直接发送 ProcessContext
            producerService.sendAttachmentTask(ctx);

            log.info("【Kafka模式】成功提交子任务 {} 的附件处理消息到Kafka", ctx.subTaskId);

        } catch (Exception e) {
            log.error("【Kafka模式】处理子任务 {} 的数据失败（发送消息时异常）: {}", ctx.subTaskId, e.getMessage(), e);
            // 降级逻辑
            handleFailure(ctx, e);
        }
    }

    /**
     * 处理失败的降级逻辑
     */
    private void handleFailure(ProcessContext<DATA, QUERY> ctx, Exception originalException) {
        log.warn("发送失败，尝试降级为异步模式处理，SubTaskID: {}, 原因: {}", ctx.subTaskId, originalException.getMessage());
        try {
            // 降级为异步处理
            processAsyncMode(ctx);
        } catch (Exception fallbackException) {
            log.error("降级处理也失败了，SubTaskID: {}", ctx.subTaskId, fallbackException);
        }
    }

    private void processAsyncMode(ProcessContext<DATA, QUERY> ctx) {
        try {
            log.info("【异步模式】开始处理子任务 {} 的数据", ctx.subTaskId);

            // 如果是IrMessage类型，使用原有的处理逻辑
            if (!ctx.messages.isEmpty() &&
                    ctx.messages.get(0) instanceof IrMessageData && ctx.query instanceof IrMessageQuery) {
//                BatchDataProcessingService service = SpringContextUtil.getBean(
//                        "attachmentProcessingServiceImpl", BatchDataProcessingService.class);
                service.processDataAsync(ctx.messages, ctx.subTaskId, ctx.subTaskNo, ctx.query)
                        .whenComplete((result, throwable) -> {
                            if (throwable != null) {
                                log.error("【异步模式】子任务 {} 的附件处理失败: {}", ctx.subTaskId, throwable.getMessage(), throwable);
                            } else {
                                log.info("【异步模式】子任务 {} 的附件处理完成", ctx.subTaskId);
                            }
                        });
                log.info("【异步模式】子任务 {} 的附件处理已提交到线程池", ctx.subTaskId);
            } else {
                log.warn("【异步模式】不支持的数据类型，跳过处理: {}", ctx.messages.getClass().getSimpleName());
            }
        } catch (Exception e) {
            log.error("【异步模式】子任务 {} 提交数据处理失败: {}", ctx.subTaskId, e.getMessage(), e);
        }
    }

    private void processDeferredMode(ProcessContext<DATA, QUERY> ctx) {
        log.info("【延迟模式】将子任务 {} 的数据处理添加到延迟队列", ctx.subTaskId);

        Runnable task = () -> {
            try {
                log.info("【延迟模式】开始处理子任务 {} 的数据", ctx.subTaskId);

                // 如果是IrMessage类型，使用原有的处理逻辑
                if (!ctx.messages.isEmpty() &&
                        ctx.messages.get(0) instanceof IrMessageData && ctx.query instanceof IrMessageQuery) {
                    service.processData(ctx.messages, ctx.subTaskId, ctx.subTaskNo, ctx.query);
                } else {
                    log.warn("【延迟模式】不支持的数据类型，跳过处理: {}", ctx.messages.getClass().getSimpleName());
                }

                log.info("【延迟模式】子任务 {} 的数据处理完成", ctx.subTaskId);
            } catch (Exception e) {
                log.error("【延迟模式】子任务 {} 的数据处理失败: {}", ctx.subTaskId, e.getMessage(), e);
            }
        };

        // 使用computeIfAbsent确保线程安全地添加任务到列表
        deferredTasks.computeIfAbsent(ctx.mainTaskId, k -> new ArrayList<>()).add(task);
        log.info("【延迟模式】子任务 {} 的数据处理已添加到延迟队列，等待主任务完成", ctx.subTaskId);
    }

    private void processNoneMode(ProcessContext<DATA, QUERY> ctx) {
        log.info("【跳过模式】子任务 {} 不处理数据", ctx.subTaskId);
    }
}
