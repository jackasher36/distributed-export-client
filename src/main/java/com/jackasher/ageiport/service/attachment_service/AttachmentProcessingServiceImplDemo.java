package com.jackasher.ageiport.service.attachment_service;

import com.jackasher.ageiport.demo.DownloadFileParamDemo;
import com.jackasher.ageiport.model.export.ExportParams;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.jackasher.ageiport.utils.IrMessageUtils.getResolvedParams;

@Service
public class AttachmentProcessingServiceImplDemo implements AttachmentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentProcessingServiceImplDemo.class);

//    @Resource
    private MinioClient minioClient;
    
    @Resource
    @Qualifier("attachmentTaskExecutor")
    private ThreadPoolTaskExecutor attachmentTaskExecutor;

    @Override
    public void processAndPackageAttachments(List<IrMessageData> messages, String subTaskId, int pageNum, IrMessageQuery irMessageQuery) {
        log.info("开始处理子任务 {} 的附件，批次号：{}。", subTaskId, pageNum);
        log.info("接收到的消息数量: {}", messages != null ? messages.size() : 0);
        log.info("IrMessageQuery: {}", irMessageQuery);

        if (messages == null || messages.isEmpty()) {
            log.warn("子任务 {} 的批次 {} 未接收到任何消息数据，跳过处理。", subTaskId, pageNum);
            return;
        }

        // 4.  权限校验 (模拟)
        // AuthorityCheckUtils.checkDataAuthority(...);
        log.info("  [模拟] 权限校验通过...");

        // 5.  调用(模拟的)核心批量处理方法
        try {
            Boolean processAttachments = getResolvedParams(irMessageQuery).getProcessAttachments();
            if (processAttachments) {
                // TODO 你的真实业务处理逻辑
                log.info("开始处理附件...");
                //模拟事件处理中
                Thread.sleep(15000);
            } else {
                log.info("附件处理被禁用，跳过附件处理。");
            }
            log.info("子任务 {} 的批次 {} 附件处理并打包成功。", subTaskId, pageNum);
        } catch (Exception e) {
            log.error("子任务 {} 在处理批次 {} 时发生严重错误: {}", subTaskId, pageNum, e.getMessage(), e);
            // 向上抛出异常，让 convert 方法捕获
            throw new RuntimeException("处理附件并打包时失败，子任务: " + subTaskId + ", 批次: " + pageNum, e);
        }
    }


    @Override
    public CompletableFuture<Void> processAndPackageAttachmentsAsync(List<IrMessageData> messages, String subTaskId, int pageNum, IrMessageQuery irMessageQuery) {
        return processAndPackageAttachmentsAsync(messages, subTaskId, pageNum, irMessageQuery, 300); // 默认5分钟超时
    }

    @Override
    public CompletableFuture<Void> processAndPackageAttachmentsAsync(List<IrMessageData> messages, String subTaskId, int pageNum, IrMessageQuery irMessageQuery, long timeoutSeconds) {
        log.info("开始异步处理子任务 {} 的附件，批次号：{}，超时时间：{}秒", subTaskId, pageNum, timeoutSeconds);
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // 在异步线程中执行同步处理逻辑
                processAndPackageAttachments(messages, subTaskId, pageNum, irMessageQuery);
                log.info("子任务 {} 的附件异步处理完成", subTaskId);
            } catch (Exception e) {
                log.error("子任务 {} 的附件异步处理失败: {}", subTaskId, e.getMessage(), e);
                throw new RuntimeException("异步处理附件失败，子任务: " + subTaskId, e);
            }
        }, attachmentTaskExecutor);
        
        // Java 8 兼容的超时处理
        return applyTimeout(future, timeoutSeconds, subTaskId);
    }
    
    /**
     * 为 CompletableFuture 添加超时处理（Java 8 兼容）
     */
    private CompletableFuture<Void> applyTimeout(CompletableFuture<Void> future, long timeoutSeconds, String subTaskId) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            if (!future.isDone()) {
                log.error("子任务 {} 的附件处理超时（{}秒），已被取消", subTaskId, timeoutSeconds);
                future.cancel(true);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);
        
        return future.whenComplete((result, throwable) -> {
            timeoutFuture.cancel(false);
            scheduler.shutdown();
            if (throwable != null) {
                if (throwable instanceof CancellationException) {
                    log.error("子任务 {} 的附件处理被取消", subTaskId);
                } else {
                    log.error("子任务 {} 的附件异步处理出现异常: {}", subTaskId, throwable.getMessage());
                }
            }
        });
    }
}
