package com.jackasher.ageiport.mq.kafka;

import com.jackasher.ageiport.service.data_processing_service.BatchDataProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Kafka 消费者服务
 * @author Jackasher
 */
@Service
@ConditionalOnProperty(name = "ageiport.export.attachment-process-mode", havingValue = "kafka")
public class KafkaConsumerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Resource(name = "attachmentProcessingServiceImpl")
    private BatchDataProcessingService batchDataProcessingService;
    
    /**
     * 监听附件处理任务
     * @param message 附件任务消息
     * @param partition 分区
     * @param offset 偏移量
     * @param ack 手动确认
     */
    @KafkaListener(topics = KafkaProducerService.ATTACHMENT_TOPIC, groupId = "attachment-processing-group")
    public void handleAttachmentTask(@Payload AttachmentTaskMessage message,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment ack) {
        try {
            log.info("接收到附件处理任务，SubTaskID: {}, Partition: {}, Offset: {}, 消息数量: {}", 
                message.getSubTaskId(), partition, offset, 
                message.getMessages() != null ? message.getMessages().size() : 0);
            
            // 处理附件任务
            processAttachmentTask(message);
            
            // 手动确认消息处理完成
            ack.acknowledge();
            
            log.info("附件处理任务完成，SubTaskID: {}", message.getSubTaskId());
            
        } catch (Exception e) {
            log.error("处理附件任务失败，SubTaskID: {}, Partition: {}, Offset: {}", 
                message.getSubTaskId(), partition, offset, e);
            
            // 这里可以实现错误处理策略：
            // 1. 重试机制
            // 2. 发送到死信队列
            // 3. 记录错误日志
            // 
            // 注意：如果不调用 ack.acknowledge()，消息将不会被确认
            // 可以根据业务需求决定是否确认消息
            
            // 根据错误类型决定是否确认消息
            if (shouldAcknowledgeOnError(e)) {
                ack.acknowledge();
                log.warn("虽然处理失败，但已确认消息以避免重复处理，SubTaskID: {}", message.getSubTaskId());
            } else {
                log.warn("处理失败，消息将保持未确认状态以便重试，SubTaskID: {}", message.getSubTaskId());
            }
        }
    }
    
    /**
     * 处理附件任务的核心逻辑
     * @param message 附件任务消息
     */
    private void processAttachmentTask(AttachmentTaskMessage message) {
        try {
            String subTaskId = message.getSubTaskId();
            
            if (batchDataProcessingService != null && message.getMessages() != null && !message.getMessages().isEmpty()) {
                log.info("开始处理附件，SubTaskID: {}, 文件数量: {}", 
                    message.getSubTaskId(), message.getMessages().size());
                
                // 调用附件处理服务
                // 调用核心业务逻辑执行附件处理
                batchDataProcessingService.processData(
                        message.getMessages(),
                        subTaskId,
                        message.getSubTaskNo(),
                        message.getQuery()
                );
                
                log.info("附件处理完成，SubTaskID: {}", message.getSubTaskId());
            } else {
                log.warn("无法获取附件处理服务或消息为空，SubTaskID: {}", message.getSubTaskId());
            }
            
        } catch (Exception e) {
            log.error("处理附件时发生异常，SubTaskID: {}", message.getSubTaskId(), e);
            throw e; // 重新抛出异常，让上层处理
        }
    }
    
    /**
     * 判断在发生错误时是否应该确认消息
     * @param e 异常
     * @return true 如果应该确认消息（避免重复处理），false 如果应该重试
     */
    private boolean shouldAcknowledgeOnError(Exception e) {
        // 根据异常类型判断是否应该重试
        // 例如：网络错误可以重试，数据格式错误则不应该重试

        // 数据问题，不应该重试
        return e instanceof IllegalArgumentException ||
                e instanceof NullPointerException;
        
        // 其他异常可能是临时性问题，可以重试
    }
    
    /**
     * 测试消息监听器
     * @param message 消息内容
     * @param ack 确认
     */
    @KafkaListener(topics = "test-topic", groupId = "test-group")
    public void handleTestMessage(@Payload String message, Acknowledgment ack) {
        try {
            log.info("接收到测试消息: {}", message);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理测试消息失败: {}", message, e);
        }
    }
}
