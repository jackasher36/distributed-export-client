// src/main/java/com/jackasher/ageiport/service/callback_service/trigger/HttpDeferredTaskTriggerStrategy.java

package com.jackasher.ageiport.service.trigger;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.controller.monitor.InternalTaskController;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HttpDeferredTaskTriggerStrategy implements DeferredTaskTriggerStrategy {

    public static final String STRATEGY_NAME = "http";

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private RestTemplate restTemplate; // 确保已配置RestTemplate Bean

    @Value("${spring.application.name}")
    private String applicationName;

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    @Override
    public void trigger(MainTask mainTask) {
        String mainTaskId = mainTask.getMainTaskId();
        log.info("[HTTP-Trigger] 开始广播任务完成事件, MainTaskID: {}", mainTaskId);

        // 从服务发现获取所有实例
        List<ServiceInstance> instances = discoveryClient.getInstances(applicationName); // 修正服务名
        
        // 调试信息：打印所有实例
        log.info("[HTTP-Trigger] 查询服务名: ageiport, 发现实例数量: {}", instances.size());
        for (ServiceInstance instance : instances) {
            log.info("[HTTP-Trigger] 发现实例: {}", instance);
        }
        
        if (instances.isEmpty()) {
            log.error("[HTTP-Trigger] 无法找到任何服务实例来广播事件!");
            return;
        }

        log.info("[HTTP-Trigger] 发现 {} 个服务实例，准备广播...", instances.size());

        InternalTaskController.TriggerPayload payload = new InternalTaskController.TriggerPayload();
        payload.setMainTaskId(mainTaskId);

        for (ServiceInstance instance : instances) {
            String url = instance.getUri() + "/internal/api/task/trigger-deferred";
            try {
                restTemplate.postForEntity(url, payload, String.class);
                log.info("[HTTP-Trigger] 成功通知节点: {}", instance.getUri());
            } catch (Exception e) {
                log.error("[HTTP-Trigger] 通知节点 {} 失败. MainTaskID: {}", instance.getUri(), mainTaskId, e);
                // 生产环境需要加入重试或失败记录机制
            }
        }
    }
}