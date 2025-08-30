package com.jackasher.ageiport.mq.kafka;

import java.util.List;

import javax.annotation.Resource;

import com.jackasher.ageiport.model.export.GenericExportQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.jackasher.ageiport.model.dto.ProcessContext;

/**
 * Kafka 生产者服务
 * @author Jackasher
 */
@Service
public class KafkaProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);
    
    // 定义主题名称
    public static final String ATTACHMENT_TOPIC = "attachment-processing-topic";

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * 发送附件处理任务消息到 Kafka
     * @param message 附件任务消息
     */
    public <DATA, QUERY extends com.jackasher.ageiport.model.export.GenericExportQuery> void sendAttachmentTask(ProcessContext<DATA, QUERY> message) {
        try {
            log.info("准备发送附件处理任务到Kafka，SubTaskID: {}, 消息数量: {}", 
                message.subTaskId, 
                message.messages != null ? message.messages.size() : 0);
            
            // 检查消息是否需要分片（5MB 限制）
            if (MessageSplitter.needsSplit(message, 5 * 1024 * 1024)) {
                log.info("消息过大，进行分片处理，SubTaskID: {}", message.subTaskId);
                sendMessageWithSplit(message);
            } else {
                log.debug("消息大小适中，直接发送，SubTaskID: {}", message.subTaskId);
                sendSingleMessage(message);
            }
            
        } catch (Exception e) {
            log.error("发送附件处理任务到Kafka失败，SubTaskID: {}", message.subTaskId, e);
            handleSendFailure(message, e);
        }
    }
    
    /**
     * 发送单个消息
     */
    private <DATA, QUERY extends GenericExportQuery> void sendSingleMessage(ProcessContext<DATA, QUERY> message) {
        // 使用 subTaskId 作为分区键，确保同一子任务的消息发送到同一分区
        ListenableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(ATTACHMENT_TOPIC, message.subTaskId, message);
        
        // 添加回调处理发送结果
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onSuccess(SendResult<String, Object> result) {
                log.info("成功发送附件处理任务到Kafka，SubTaskID: {}, Partition: {}, Offset: {}", 
                    message.subTaskId, 
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("发送附件处理任务到Kafka失败，SubTaskID: {}", message.subTaskId, ex);
                handleSendFailure(message, ex);
            }
        });
    }
    
    /**
     * 分片发送消息
     */
    private <DATA, QUERY extends GenericExportQuery> void sendMessageWithSplit(ProcessContext<DATA, QUERY> message) {
        List<ProcessContext<DATA, QUERY>> chunks = MessageSplitter.splitMessage(message);
        
        log.info("开始分片发送，总分片数: {}, 原始SubTaskID: {}", chunks.size(), message.subTaskId);
        
        for (ProcessContext<DATA, QUERY> chunk : chunks) {
            sendSingleMessage(chunk);
        }
        
        log.info("分片发送完成，总分片数: {}, 原始SubTaskID: {}", chunks.size(), message.subTaskId);
    }
    
    /**
     * 处理发送失败的情况
     * @param message 失败的消息
     * @param ex 异常信息
     */
    private <DATA, QUERY extends com.jackasher.ageiport.model.export.GenericExportQuery> void handleSendFailure(ProcessContext<DATA, QUERY> message, Throwable ex) {
        // 可以实现以下策略：
        // 1. 重试机制
        // 2. 降级为同步处理
        // 3. 写入死信队列
        // 4. 记录到数据库等待后续处理
        
        log.warn("附件处理任务发送失败，SubTaskID: {}，可以考虑降级为同步处理", message.subTaskId);
        
        // 这里可以调用同步处理的备用方案
        // fallbackToSyncProcessing(message);
    }
    
    /**
     * 发送简单的测试消息
     * @param topic 主题
     * @param key 键
     * @param message 消息内容
     */
    public void sendMessage(String topic, String key, Object message) {
        try {
            log.info("发送消息到Kafka主题: {}, 键: {}", topic, key);
            kafkaTemplate.send(topic, key, message);
        } catch (Exception e) {
            log.error("发送消息到Kafka失败，主题: {}, 键: {}", topic, key, e);
        }
    }
}
