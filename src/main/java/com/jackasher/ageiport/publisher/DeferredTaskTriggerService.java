// src/main/java/com/jackasher/ageiport/service/callback_service/trigger/DeferredTaskTriggerService.java

package com.jackasher.ageiport.publisher;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.config.export.ExportProperties;

@Service
public class DeferredTaskTriggerService {

    @Value("${ageiport.export.deferred-trigger-strategy:http}") // 默认为http
    private String configuredStrategy;

    @Resource
    ExportProperties exportProperties;

    private final Map<String, DeferredTaskTriggerStrategy> strategies;

    // 构造函数注入所有策略实现
    public DeferredTaskTriggerService(List<DeferredTaskTriggerStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(DeferredTaskTriggerStrategy::getStrategyName, Function.identity()));
    }

    public void trigger(MainTask mainTask) {
        // 每次触发时动态获取当前配置
        String currentStrategy = exportProperties.getDeferredTriggerStrategy();
        System.out.println("【延迟触发策略】当前配置策略: " + currentStrategy);
        
        DeferredTaskTriggerStrategy strategy = strategies.get(currentStrategy);
        if (strategy == null) {
            System.out.println("【延迟触发策略】未找到策略 " + currentStrategy + "，回退到http策略");
            strategy = strategies.get("http");
        }
        
        System.out.println("【延迟触发策略】使用策略: " + strategy.getStrategyName());
        strategy.trigger(mainTask);
    }
}