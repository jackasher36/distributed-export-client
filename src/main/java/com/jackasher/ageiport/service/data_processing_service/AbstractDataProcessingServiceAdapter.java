package com.jackasher.ageiport.service.data_processing_service;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.jackasher.ageiport.model.export.GenericExportQuery;

/**
 * 通用数据处理服务适配器基类
 * 提供同步/异步处理的通用逻辑，具体业务处理由子类实现
 * 
 * @param <DATA> 数据类型
 * @param <QUERY> 查询类型
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractDataProcessingServiceAdapter<DATA, QUERY extends GenericExportQuery>
        implements GenericDataProcessingService<DATA, QUERY> {

    private static final Logger log = LoggerFactory.getLogger(AbstractDataProcessingServiceAdapter.class);

    private static final ScheduledExecutorService TIMEOUT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DataProcessingTimeoutScheduler");
                t.setDaemon(true); // 设为守护线程
                return t;
            });

    @Autowired(required = false)
    @Qualifier("attachmentTaskExecutor")
    private ThreadPoolTaskExecutor attachmentTaskExecutor;

    @Override
    public void processData(List<DATA> data, String subTaskId, int pageNum, QUERY query) {
        log.info("开始处理子任务 {} 的数据，批次号：{}", subTaskId, pageNum);
        log.info("接收到的数据: {}", data != null ? data.getClass().getSimpleName() : "null");
        log.info("查询参数: {}", query);

        if (data == null) {
            log.warn("子任务 {} 的批次 {} 未接收到任何数据，跳过处理", subTaskId, pageNum);
            return;
        }

        try {
            // 调用子类的具体业务处理逻辑
            doProcessData(data, subTaskId, pageNum, query);
            log.info("子任务 {} 的批次 {} 数据处理成功", subTaskId, pageNum);
        } catch (Exception e) {
            log.error("子任务 {} 在处理批次 {} 时发生严重错误: {}", subTaskId, pageNum, e.getMessage(), e);
            throw new RuntimeException("处理数据时失败，子任务: " + subTaskId + ", 批次: " + pageNum, e);
        }
    }

    @Override
    public CompletableFuture<Void> processDataAsync(List<DATA> data, String subTaskId, int pageNum, QUERY query) {
        return processDataAsync(data, subTaskId, pageNum, query, 300); // 默认5分钟超时
    }

    @Override
    public CompletableFuture<Void> processDataAsync(List<DATA> data, String subTaskId, int pageNum, QUERY query, long timeoutSeconds) {
        log.info("开始异步处理子任务 {} 的数据，批次号：{}，超时时间：{}秒", subTaskId, pageNum, timeoutSeconds);

        CompletableFuture<Void> future;
        
        if (attachmentTaskExecutor != null) {
            // 使用专用线程池异步处理
            log.debug("使用专用线程池 attachmentTaskExecutor 进行异步处理");
            future = CompletableFuture.runAsync(() -> {
                try {
                    processData(data, subTaskId, pageNum, query);
                    log.info("子任务 {} 的数据异步处理完成", subTaskId);
                } catch (Exception e) {
                    log.error("子任务 {} 的数据异步处理失败: {}", subTaskId, e.getMessage(), e);
                    throw new RuntimeException("异步处理数据失败，子任务: " + subTaskId, e);
                }
            }, attachmentTaskExecutor);
        } else {
            // 降级到默认线程池
            log.warn("专用线程池不可用，降级到默认 ForkJoinPool 进行异步处理");
            future = CompletableFuture.runAsync(() -> {
                try {
                    processData(data, subTaskId, pageNum, query);
                    log.info("子任务 {} 的数据异步处理完成（使用默认线程池）", subTaskId);
                } catch (Exception e) {
                    log.error("子任务 {} 的数据异步处理失败: {}", subTaskId, e.getMessage(), e);
                    throw new RuntimeException("异步处理数据失败，子任务: " + subTaskId, e);
                }
            });
        }

        // Java 8 兼容的超时处理
        return applyTimeout(future, timeoutSeconds, subTaskId);
    }

    /**
     * 具体的业务处理逻辑，由子类实现
     * 
     * @param data 要处理的数据
     * @param subTaskId 子任务ID
     * @param pageNum 页码/批次号
     * @param query 查询参数
     * @throws Exception 处理异常
     */
    protected abstract void doProcessData(List<DATA> data, String subTaskId, int pageNum, QUERY query) throws Exception;

    /**
     * 为 CompletableFuture 添加超时处理（Java 8 兼容）
     */
    private CompletableFuture<Void> applyTimeout(CompletableFuture<Void> future, long timeoutSeconds, String subTaskId) {
        ScheduledFuture<?> timeoutFuture = TIMEOUT_SCHEDULER.schedule(() -> {
            if (!future.isDone()) {
                log.error("子任务 {} 的数据处理超时（{}秒），已被取消", subTaskId, timeoutSeconds);
                future.cancel(true);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        return future.whenComplete((result, throwable) -> {
            timeoutFuture.cancel(false);
            if (throwable != null) {
                if (throwable instanceof CancellationException) {
                    log.error("子任务 {} 的数据处理被取消", subTaskId);
                } else {
                    log.error("子任务 {} 的数据异步处理出现异常: {}", subTaskId, throwable.getMessage());
                }
            }
        });
    }
}
