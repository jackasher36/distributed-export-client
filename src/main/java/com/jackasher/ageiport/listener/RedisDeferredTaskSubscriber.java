// src/main/java/com/jackasher/ageiport/service/callback_service/trigger/RedisDeferredTaskSubscriber.java

package com.jackasher.ageiport.listener;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.jackasher.ageiport.dispatcher.GenericProcessingDispatcher;
import com.jackasher.ageiport.utils.network.NetworkUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis 订阅者,接受主节点的信号,并触发延迟任务
 */
@Component
@Slf4j
public class RedisDeferredTaskSubscriber {

    @Resource
    @Qualifier("serialAttachmentTaskExecutor")
    private Executor serialExecutor;

    // 此方法名需要与 MessageListenerAdapter 中配置的匹配
    public void handleMessage(String mainTaskId) {
        log.info("[Redis-Subscriber] 节点 {} 收到触发指令, MainTaskID: {}", NetworkUtils.getLocalIP(), mainTaskId);
        
        // 使用单线程执行器来保证本节点内任务的串行执行
        ((ExecutorService) serialExecutor).submit(() -> {
            GenericProcessingDispatcher.triggerDeferredTasksSerially(mainTaskId, (ExecutorService) serialExecutor);
        });
    }
}