// src/main/java/com/jackasher/ageiport/service/callback_service/trigger/DeferredTaskTriggerStrategy.java

package com.jackasher.ageiport.service.trigger;

import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;

/**
 * 延迟任务触发器策略接口
 */
public interface DeferredTaskTriggerStrategy {
    void trigger(MainTask mainTask);
    String getStrategyName();
}