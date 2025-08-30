// src/main/java/com/jackasher/ageiport/service/mq/MqProducerService.java
package com.jackasher.ageiport.mq.rabbitmq;

import javax.annotation.Resource;

import com.jackasher.ageiport.model.export.GenericExportQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.jackasher.ageiport.model.dto.ProcessContext;

@Service
public class MqProducerService {
    
    private static final Logger log = LoggerFactory.getLogger(MqProducerService.class);
    
    // 定义交换机和路由键
    public static final String ATTACHMENT_EXCHANGE = "attachment_exchange";
    public static final String ATTACHMENT_ROUTING_KEY = "attachment.process.task";
    
    @Resource
    private RabbitTemplate rabbitTemplate;
    
    public <DATA, QUERY extends GenericExportQuery> void sendAttachmentTask(ProcessContext<DATA, QUERY> message) {
        try {
            log.info("准备发送附件处理任务到MQ，SubTaskID: {}", message.subTaskId);
            rabbitTemplate.convertAndSend(ATTACHMENT_EXCHANGE, ATTACHMENT_ROUTING_KEY, message);
            log.info("成功发送附件处理任务到MQ，SubTaskID: {}", message.subTaskId);
        } catch (Exception e) {
            log.error("发送附件处理任务到MQ失败, SubTaskID: {}", message.subTaskId, e);
            // 这里可以增加失败处理逻辑，例如降级为异步执行或记录失败
        }
    }
}