package com.jackasher.ageiport.config.thread;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 线程池配置类
 * 用于配置附件处理等耗时操作的专用线程池
 *
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "ageiport.thread-pool.attachment")
@Data
public class ThreadPoolConfig {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    /**
     * 核心线程数，默认为CPU核心数
     */
    private int corePoolSize = Runtime.getRuntime().availableProcessors();

    /**
     * 最大线程数，默认为CPU核心数的2倍
     */
    private int maxPoolSize = Runtime.getRuntime().availableProcessors() * 2;

    /**
     * 队列容量，默认200
     */
    private int queueCapacity = 200;

    /**
     * 线程空闲时间，默认60秒
     */
    private int keepAliveSeconds = 60;

    /**
     * 线程名前缀
     */
    private String threadNamePrefix = "attachment-executor-";

    /**
     * 创建附件处理专用的线程池
     */
    @Bean("attachmentTaskExecutor")
    public ThreadPoolTaskExecutor attachmentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // 设置拒绝策略：当线程池和队列都满时，由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 设置等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("附件处理线程池初始化完成 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }

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
