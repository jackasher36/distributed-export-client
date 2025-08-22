// src/main/java/com/jackasher/ageiport/controller/internal/InternalTaskController.java

package com.jackasher.ageiport.listener;

import com.jackasher.ageiport.utils.business.AttachmentProcessUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/internal/api/task")
@Slf4j
public class InternalTaskController {

    @Resource
    private Executor serialAttachmentTaskExecutor;

    @PostMapping("/trigger-deferred")
    public ResponseEntity<String> triggerDeferredTask(@RequestBody TriggerPayload payload) {
        log.info("收到Master节点HTTP指令，触发本节点对 mainTaskId: {} 的延迟任务检查", payload.getMainTaskId());
            // 异步执行，立即返回，不阻塞Master节点的回调线程
            ((ExecutorService) serialAttachmentTaskExecutor).submit(() -> {
                AttachmentProcessUtil.triggerDeferredTasksSerially(payload.getMainTaskId(), (ExecutorService) serialAttachmentTaskExecutor);
            });
            return ResponseEntity.ok("指令已接收 for mainTaskId: " + payload.getMainTaskId());
    }
    @Data
    public static class TriggerPayload implements Serializable {
        private static final long serialVersionUID = 1L;
        private String mainTaskId;
    }
}