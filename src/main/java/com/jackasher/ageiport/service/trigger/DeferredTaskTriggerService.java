// src/main/java/com/jackasher/ageiport/service/callback_service/trigger/DeferredTaskTriggerService.java

package com.jackasher.ageiport.service.trigger;

import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.config.export.ExportProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeferredTaskTriggerService {

    @Value("${ageiport.export.deferred-trigger-strategy:http}") // 默认为http
    private String configuredStrategy;

    @Resource
    ExportProperties exportProperties;

    private final Map<String, DeferredTaskTriggerStrategy> strategies;
    private DeferredTaskTriggerStrategy selectedStrategy;

    // 构造函数注入所有策略实现
    public DeferredTaskTriggerService(List<DeferredTaskTriggerStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(DeferredTaskTriggerStrategy::getStrategyName, Function.identity()));
    }

    @PostConstruct
    public void init() {
        configuredStrategy = exportProperties.getDeferredTriggerStrategy();
        this.selectedStrategy = strategies.get(configuredStrategy);
        if (this.selectedStrategy == null) {
            throw new IllegalStateException("未找到配置的延迟任务触发策略: " + configuredStrategy);
        }
    }

    public void trigger(MainTask mainTask) {
        selectedStrategy.trigger(mainTask);
    }
}