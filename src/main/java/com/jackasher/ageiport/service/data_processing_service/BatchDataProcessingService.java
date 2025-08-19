package com.jackasher.ageiport.service.data_processing_service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;

/**
 * 附件处理服务接口
 */
public interface BatchDataProcessingService {
    
    /**
     * 同步处理附件（原有方法，保持向后兼容）
     * 根据一批消息记录，处理其关联的附件，并生成打包后的ZIP文件。
     * @param messages 待处理的消息列表
     * @param subTaskId 当前子任务ID，用于构建唯一的输出目录
     * @param pageNum 当前处理的页码/批次号
     * @param irMessageQuery 查询参数
     */
    void processData(List<IrMessageData> messages, String subTaskId, int pageNum, IrMessageQuery irMessageQuery);
    
    /**
     * 异步处理附件（新增方法）
     * 使用线程池异步处理附件，不阻塞主流程
     * @param messages 待处理的消息列表
     * @param subTaskId 当前子任务ID，用于构建唯一的输出目录
     * @param pageNum 当前处理的页码/批次号
     * @param irMessageQuery 查询参数
     * @return CompletableFuture，可用于监控处理状态和结果
     */
    CompletableFuture<Void> processDataAsync(List<IrMessageData> messages, String subTaskId, int pageNum, IrMessageQuery irMessageQuery);
    
    /**
     * 异步处理附件，带有超时控制
     * @param messages 待处理的消息列表
     * @param subTaskId 当前子任务ID
     * @param pageNum 当前处理的页码/批次号
     * @param irMessageQuery 查询参数
     * @param timeoutSeconds 超时时间（秒）
     * @return CompletableFuture，带超时控制
     */
    CompletableFuture<Void> processDataAsync(List<IrMessageData> messages, String subTaskId, int pageNum, IrMessageQuery irMessageQuery, long timeoutSeconds);
}