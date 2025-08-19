package com.jackasher.ageiport.utils.business;

import com.jackasher.ageiport.constant.AttachmentProcessMode;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.service.attachment_service.AttachmentProcessingService;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * 附件处理工具类 - 简化附件处理逻辑
 * 使用策略模式 + 函数式编程，优雅替代switch语句
 * 
 * @author jackasher
 */
public class AttachmentProcessUtil {
    
    private static final Logger log = LoggerFactory.getLogger(AttachmentProcessUtil.class);
    
    // 存储延迟处理的任务 - 一个主任务对应多个子任务
    private static final Map<String, List<Runnable>> deferredTasks = new ConcurrentHashMap<>();
    
    // 处理器映射 - 每种模式对应一个处理器（优雅替代switch）
    private static final Map<AttachmentProcessMode, ProcessHandler> handlers = new EnumMap<>(AttachmentProcessMode.class);
    
    static {
        // 初始化处理器映射 - 使用方法引用，简洁优雅
        handlers.put(AttachmentProcessMode.SYNC, AttachmentProcessUtil::processSyncMode);
        handlers.put(AttachmentProcessMode.ASYNC, AttachmentProcessUtil::processAsyncMode);
        handlers.put(AttachmentProcessMode.DEFERRED, AttachmentProcessUtil::processDeferredMode);
        handlers.put(AttachmentProcessMode.NONE, AttachmentProcessUtil::processNoneMode);
    }
    
    @FunctionalInterface
    private interface ProcessHandler {
        void handle(ProcessContext context);
    }
    
    // 处理上下文 - 封装所有参数，避免长参数列表
    private static class ProcessContext {
        final List<IrMessageData> messages;
        final String subTaskId;
        final int subTaskNo;
        final IrMessageQuery query;
        final String mainTaskId;
        
        ProcessContext(List<IrMessageData> messages, String subTaskId, int subTaskNo, 
                      IrMessageQuery query, String mainTaskId) {
            this.messages = messages;
            this.subTaskId = subTaskId;
            this.subTaskNo = subTaskNo;
            this.query = query;
            this.mainTaskId = mainTaskId;
        }
    }
    
    /**
     * 处理附件 - 根据配置选择处理方式
     * 
     * @param messages 消息数据
     * @param subTaskId 子任务ID
     * @param subTaskNo 子任务编号
     * @param query 查询参数
     * @param mainTaskId 主任务ID
     */
    public static void processAttachments(List<IrMessageData> messages, String subTaskId, 
                                        int subTaskNo, IrMessageQuery query, String mainTaskId) {
        
        Boolean processAttachments = IrMessageUtils.getResolvedParams(query).getProcessAttachments();
        if (processAttachments == null || !processAttachments) {
            log.info("子任务 {} 跳过附件处理", subTaskId);
            return;
        }
        
        String modeStr = IrMessageUtils.getResolvedParams(query).getAttachmentProcessMode();
        AttachmentProcessMode mode = parseMode(modeStr);
        
        log.info("子任务 {} 使用附件处理模式: {}", subTaskId, mode);
        
        // 使用映射表 + 函数式编程替代switch
        ProcessContext context = new ProcessContext(messages, subTaskId, subTaskNo, query, mainTaskId);
        handlers.getOrDefault(mode, AttachmentProcessUtil::processNoneMode).handle(context);
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
     * @param mainTaskId 主任务ID
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

        } catch (Exception e) {
            // InterruptedException, ExecutionException, CancellationException 都会被捕获
            log.error("附件处理链中有一个任务失败，后续所有任务将不再执行。MainTaskID: {}, 失败原因: {}",
                    mainTaskId, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            // 出现异常，递归链中断，不再提交新任务。
        }
    }
    
    private static AttachmentProcessMode parseMode(String modeStr) {
        if (modeStr == null || modeStr.trim().isEmpty()) {
            return AttachmentProcessMode.ASYNC; // 默认异步
        }
        
        try {
            return AttachmentProcessMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的附件处理模式: {}, 使用默认的ASYNC模式", modeStr);
            return AttachmentProcessMode.ASYNC;
        }
    }
    
    // ==================== 具体处理策略实现 ====================
    
    private static void processSyncMode(ProcessContext ctx) {
        try {
            log.info("【同步模式】开始处理子任务 {} 的附件", ctx.subTaskId);
            AttachmentProcessingService service = SpringContextUtil.getBean(
                "attachmentProcessingServiceImpl", AttachmentProcessingService.class);
            service.processAndPackageAttachments(ctx.messages, ctx.subTaskId, ctx.subTaskNo, ctx.query);
            log.info("【同步模式】子任务 {} 的附件处理完成", ctx.subTaskId);
        } catch (Exception e) {
            log.error("【同步模式】子任务 {} 的附件处理失败: {}", ctx.subTaskId, e.getMessage(), e);
        }
    }
    
    private static void processAsyncMode(ProcessContext ctx) {
        try {
            log.info("【异步模式】开始处理子任务 {} 的附件", ctx.subTaskId);
            AttachmentProcessingService service = SpringContextUtil.getBean(
                "attachmentProcessingServiceImpl", AttachmentProcessingService.class);
            service.processAndPackageAttachmentsAsync(ctx.messages, ctx.subTaskId, ctx.subTaskNo, ctx.query)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("【异步模式】子任务 {} 的附件处理失败: {}", ctx.subTaskId, throwable.getMessage(), throwable);
                    } else {
                        log.info("【异步模式】子任务 {} 的附件处理完成", ctx.subTaskId);
                    }
                });
            log.info("【异步模式】子任务 {} 的附件处理已提交到线程池", ctx.subTaskId);
        } catch (Exception e) {
            log.error("【异步模式】子任务 {} 提交附件处理失败: {}", ctx.subTaskId, e.getMessage(), e);
        }
    }
    
    private static void processDeferredMode(ProcessContext ctx) {
        log.info("【延迟模式】将子任务 {} 的附件处理添加到延迟队列", ctx.subTaskId);
        
        Runnable task = () -> {
            try {
                log.info("【延迟模式】开始处理子任务 {} 的附件", ctx.subTaskId);
                AttachmentProcessingService service = SpringContextUtil.getBean(
                    "attachmentProcessingServiceImpl", AttachmentProcessingService.class);
                service.processAndPackageAttachments(ctx.messages, ctx.subTaskId, ctx.subTaskNo, ctx.query);
                log.info("【延迟模式】子任务 {} 的附件处理完成", ctx.subTaskId);
            } catch (Exception e) {
                log.error("【延迟模式】子任务 {} 的附件处理失败: {}", ctx.subTaskId, e.getMessage(), e);
            }
        };
        
        // 使用computeIfAbsent确保线程安全地添加任务到列表
        deferredTasks.computeIfAbsent(ctx.mainTaskId, k -> new ArrayList<>()).add(task);
        log.info("【延迟模式】子任务 {} 的附件处理已添加到延迟队列，等待主任务完成", ctx.subTaskId);
    }
    
    private static void processNoneMode(ProcessContext ctx) {
        log.info("【跳过模式】子任务 {} 不处理附件", ctx.subTaskId);
    }
}