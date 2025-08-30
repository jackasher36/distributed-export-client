// src/main/java/com/jackasher/ageiport/service/callback_service/trigger/HttpDeferredTaskTriggerStrategy.java

package com.jackasher.ageiport.publisher;

import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.listener.InternalTaskController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class HttpDeferredTaskTriggerStrategy implements DeferredTaskTriggerStrategy {

    public static final String STRATEGY_NAME = "http";

    @Resource
    private DiscoveryClient discoveryClient;

    @Resource
    private RestTemplate restTemplate; // 确保已配置RestTemplate Bean



    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public void trigger(MainTask mainTask) {
        String mainTaskId = mainTask.getMainTaskId();
        log.info("[HTTP-Trigger] 开始广播任务完成事件, MainTaskID: {}", mainTaskId);

        // 从服务发现获取所有实例
        List<ServiceInstance> instances = discoveryClient.getInstances("ageiport-client"); // 你的服务名
        if (instances.isEmpty()) {
            log.error("[HTTP-Trigger] 无法找到任何服务实例来广播事件!");
            System.out.println(instances.size());
            for (ServiceInstance instance : instances) {
                System.out.println(instance);
            }
            return;
        }

        log.info("[HTTP-Trigger] 发现 {} 个服务实例，准备广播...", instances.size());

        InternalTaskController.TriggerPayload payload = new InternalTaskController.TriggerPayload();
        payload.setMainTaskId(mainTaskId);

        // 遍历实例，通知触发任务
        for (ServiceInstance instance : instances) {
            String url = instance.getUri() + "/internal/api/task/trigger-deferred";
            // 每个实例独立的重试计数器
            int currentInstanceRetryCount = 0;
            // 标记当前实例是否通知成功
            boolean notifiedSuccessfully = false;

            // 最多尝试3次（1次初次尝试 + 2次重试）
            while (currentInstanceRetryCount < 3) {
                try {
                    ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(url, payload, String.class);
                    if (stringResponseEntity.getBody() != null) {
                        log.info("[HTTP-Trigger] 成功通知节点: {} (第 {} 次尝试)", instance.getUri(), currentInstanceRetryCount + 1);
                        notifiedSuccessfully = true;
                        break;
                    } else {
                        log.warn("[HTTP-Trigger] 节点 {} 响应为空 (第 {} 次尝试), 正在重试...", instance.getUri(), currentInstanceRetryCount + 1);
                    }
                } catch (Exception e) {
                    log.error("[HTTP-Trigger] 通知节点 {} 失败 (第 {} 次尝试). MainTaskID: {}, 错误: {}", instance.getUri(), currentInstanceRetryCount + 1, mainTaskId, e.getMessage());
                    // 可以在这里加入短暂的延迟，避免立即重试
                    try {
                        Thread.sleep(1000L * (currentInstanceRetryCount + 1));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
                currentInstanceRetryCount++;
            }

            if (!notifiedSuccessfully) {
                log.error("[HTTP-Trigger] 节点 {} 经过 {} 次尝试后仍未能成功通知. MainTaskID: {}", instance.getUri(), currentInstanceRetryCount, mainTaskId);
                // TODO:可以在这里记录失败的实例，以便后续处理或告警

            }
        }



    }
}