package com.jackasher.ageiport.mq.kafka;

import com.jackasher.ageiport.model.ir_message.IrMessageData;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息分片器 - 用于处理大消息
 * @author Jackasher
 */
@Slf4j
public class MessageSplitter {
    
    // 单个消息片段的最大记录数（可调节）
    private static final int MAX_RECORDS_PER_CHUNK = 1000;
    
    /**
     * 将大的附件任务消息分片为多个较小的消息
     * @param originalMessage 原始消息
     * @return 分片后的消息列表
     */
    public static List<AttachmentTaskMessage> splitMessage(AttachmentTaskMessage originalMessage) {
        List<AttachmentTaskMessage> chunks = new ArrayList<>();
        
        if (originalMessage.getMessages() == null || originalMessage.getMessages().isEmpty()) {
            log.warn("消息为空，无需分片，SubTaskID: {}", originalMessage.getSubTaskId());
            chunks.add(originalMessage);
            return chunks;
        }
        
        List<IrMessageData> allMessages = originalMessage.getMessages();
        int totalSize = allMessages.size();
        
        // 如果消息数量较小，不需要分片
        if (totalSize <= MAX_RECORDS_PER_CHUNK) {
            log.debug("消息数量较小({})，无需分片，SubTaskID: {}", totalSize, originalMessage.getSubTaskId());
            chunks.add(originalMessage);
            return chunks;
        }
        
        log.info("开始分片处理，总记录数: {}, 子任务ID: {}", totalSize, originalMessage.getSubTaskId());
        
        // 计算分片数量
        int chunkCount = (totalSize + MAX_RECORDS_PER_CHUNK - 1) / MAX_RECORDS_PER_CHUNK;
        
        for (int i = 0; i < chunkCount; i++) {
            int startIndex = i * MAX_RECORDS_PER_CHUNK;
            int endIndex = Math.min(startIndex + MAX_RECORDS_PER_CHUNK, totalSize);
            
            // 创建分片消息
            List<IrMessageData> chunkMessages = allMessages.subList(startIndex, endIndex);
            
            AttachmentTaskMessage chunkMessage = new AttachmentTaskMessage(
                chunkMessages,
                originalMessage.getSubTaskId() + "_chunk_" + (i + 1), // 添加分片标识
                originalMessage.getSubTaskNo(),
                originalMessage.getQuery(),
                originalMessage.getMainTaskId()
            );
            
            chunks.add(chunkMessage);
            
            log.debug("创建分片 {}/{}, 记录数: {}, 分片ID: {}", 
                i + 1, chunkCount, chunkMessages.size(), chunkMessage.getSubTaskId());
        }
        
        log.info("分片完成，总分片数: {}, 原始记录数: {}, SubTaskID: {}", 
            chunks.size(), totalSize, originalMessage.getSubTaskId());
        
        return chunks;
    }
    
    /**
     * 估算消息的大致大小（字节）
     * @param message 消息
     * @return 估算的字节大小
     */
    public static long estimateMessageSize(AttachmentTaskMessage message) {
        if (message.getMessages() == null) {
            return 1000; // 基础开销
        }
        
        // 粗略估算：每条记录约 500-1000 字节（根据实际数据调整）
        int recordCount = message.getMessages().size();
        long estimatedSize = recordCount * 800L; // 平均每条记录 800 字节
        
        // 加上基础开销（JSON 序列化、元数据等）
        estimatedSize += 2000;
        
        return estimatedSize;
    }
    
    /**
     * 检查消息是否需要分片
     * @param message 消息
     * @param maxSizeBytes 最大大小（字节）
     * @return true 如果需要分片
     */
    public static boolean needsSplit(AttachmentTaskMessage message, long maxSizeBytes) {
        long estimatedSize = estimateMessageSize(message);
        boolean needsSplit = estimatedSize > maxSizeBytes;
        
        if (needsSplit) {
            log.info("消息需要分片，估算大小: {} bytes, 最大限制: {} bytes, SubTaskID: {}", 
                estimatedSize, maxSizeBytes, message.getSubTaskId());
        }
        
        return needsSplit;
    }
}
