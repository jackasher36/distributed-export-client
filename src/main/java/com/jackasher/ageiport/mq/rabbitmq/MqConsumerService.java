// src/main/java/com/jackasher/ageiport/service/mq/MqConsumerService.java
package com.jackasher.ageiport.mq.rabbitmq;

import com.jackasher.ageiport.service.data_processing_service.BatchDataProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@RabbitListener(queues = "attachment_process_queue") // 监听指定的队列
@ConditionalOnProperty(name = "ageiport.export.attachment-process-mode", havingValue = "rabbitmq")
public class MqConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MqConsumerService.class);

    @Resource(name = "attachmentProcessingServiceImpl")
    private BatchDataProcessingService batchDataProcessingService;

    @RabbitHandler
    public void process(AttachmentTaskMessage message) {
        String subTaskId = message.getSubTaskId();
        log.info("从MQ接收到附件处理任务，SubTaskID: {}", subTaskId);
        try {
            // 调用核心业务逻辑执行附件处理
            batchDataProcessingService.processData(
                    message.getMessages(),
                    subTaskId,
                    message.getSubTaskNo(),
                    message.getQuery()
            );
            log.info("成功处理了来自MQ的附件任务，SubTaskID: {}", subTaskId);
        } catch (Exception e) {
            log.error("处理来自MQ的附件任务失败, SubTaskID: {}", subTaskId, e);
            // 这里需要有重试或死信队列机制来处理失败的任务
        }
    }
}