// src/main/java/com/jackasher/ageiport/service/callback_service/trigger/RedisDeferredTaskTriggerStrategy.java

package com.jackasher.ageiport.publisher;

import javax.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.config.redis.RedisMessageListenerConfig;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisDeferredTaskTriggerStrategy implements DeferredTaskTriggerStrategy {

    public static final String STRATEGY_NAME = "redis";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private HttpDeferredTaskTriggerStrategy httpDeferredTaskTriggerStrategy;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public void trigger(MainTask mainTask) {
        String mainTaskId = mainTask.getMainTaskId();
        log.info("[Redis-Trigger] 开始通过Pub/Sub广播任务完成事件, MainTaskID: {}", mainTaskId);
        try {
            stringRedisTemplate.convertAndSend(RedisMessageListenerConfig.DEFERRED_TASK_TRIGGER_CHANNEL, mainTaskId);
            log.info("[Redis-Trigger] 成功发布指令, Channel: {}", RedisMessageListenerConfig.DEFERRED_TASK_TRIGGER_CHANNEL);
        } catch (Exception e) {
            log.error("[Redis-Trigger] 发布指令到Redis失败. MainTaskID: {}", mainTaskId, e);
            // 降级到http方案
            log.info("[Redis-Trigger] 降级到HTTP方案");
            httpDeferredTaskTriggerStrategy.trigger(mainTask);
        }
    }

}