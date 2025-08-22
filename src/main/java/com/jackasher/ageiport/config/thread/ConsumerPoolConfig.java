package com.jackasher.ageiport.config.thread;

import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.SameLen;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author Jackasher
 * @version 1.0
 * @className ConsumerPoolConfig
 * @since 1.0
 **/
@Slf4j
@Configuration
public class ConsumerPoolConfig {
    /**
     * 基于消费者的处理线程池,默认是串行化的
     */
    @Bean("serialAttachmentTaskExecutor")
    public Executor serialAttachmentTaskExecutor() {
        log.info("串行附件处理线程池初始化完成。");
        // Executors.newSingleThreadExecutor() 会创建一个队列无界的单线程池
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "serial-attachment-executor");
            t.setDaemon(true); // 设为守护线程
            return t;
        });
    }
}
