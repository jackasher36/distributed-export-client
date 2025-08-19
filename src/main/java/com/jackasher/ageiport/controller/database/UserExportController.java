package com.jackasher.ageiport.controller.database;

import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;
import com.alibaba.ageiport.processor.core.AgeiPort;
import com.alibaba.ageiport.processor.core.spi.service.TaskExecuteParam;
import com.alibaba.ageiport.processor.core.spi.service.TaskExecuteResult;
import com.alibaba.ageiport.processor.core.spi.service.TaskService;
import com.jackasher.ageiport.constant.TaskSpecificationCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Jackasher
 * @version 1.0
 * @className UserExportController
 * @since 1.0
 **/
@RestController
@RequestMapping("/user")
public class UserExportController {
    static Logger logger = LoggerFactory.getLogger(UserExportController.class);

    @Resource
    private AgeiPort ageiPort;

    @Resource
    private DiscoveryClient discoveryClient;

    @Value("${spring.application.name}")
    private String appName;

//    @Resource
//    private RedisTemplate redisTemplate;

    @PostMapping("/task")
    public TaskExecuteResult run(@RequestBody TaskExecuteParam request) {



        request.setTaskSpecificationCode(TaskSpecificationCode.USER_CSV_EXPORT_PROCESSOR);
        int nodeCount = ageiPort.getClusterManager().getNodes().size();
        logger.info("nodeCount:{}", nodeCount);

        int size = discoveryClient.getServices().size();
        logger.info("size:{}", size);

        List<ServiceInstance> instances = discoveryClient.getInstances(appName);
        logger.info("instances:{}", instances);
        TaskService taskService = ageiPort.getTaskService();
        return taskService.executeTask(request);
    }


    @GetMapping("/ping")
    public String ping() {
        return System.currentTimeMillis() + "";
    }

}
