package com.jackasher.ageiport.constant;

import java.io.Serializable;

/**
 * 附件处理模式
 * 
 * @author jackasher
 */
public enum BatchDataProcessMode implements Serializable {
    
    /**
     * 同步处理 - 阻塞当前线程
     */
    SYNC,
    
    /**
     * 异步处理 - 立即提交到线程池
     */
    ASYNC,
    
    /**
     * 延迟处理 - 任务完成后再处理
     */
    DEFERRED,

    /**
     * MQ处理 - 通过RabbitMQ进行异步处理
     */
    RABBITMQ,

    /**
     * Kafka处理 - 通过Kafka进行异步处理
     */
    KAFKA,
    
    /**
     * 不处理
     */
    NONE
}
