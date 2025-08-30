package com.jackasher.ageiport.controller.config;

import java.util.Set;

import javax.annotation.Resource;

import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jackasher.ageiport.config.export.ExportProperties;

/**
 * 配置刷新控制器
 * @author jackasher
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/config")
public class ConfigRefreshController {


    @Resource
    private ContextRefresher contextRefresher;
    
    @Resource
    private ExportProperties exportProperties;

    /**
     * 手动刷新配置
     */
    @PostMapping("/refresh")
    public String refreshConfig() {
        System.out.println("【手动刷新】开始刷新配置...");
        System.out.println("【刷新前】batchDataProcessMode: " + exportProperties.getBatchDataProcessMode());
        
        Set<String> refreshed = contextRefresher.refresh();
        
        System.out.println("【刷新后】batchDataProcessMode: " + exportProperties.getBatchDataProcessMode());
        System.out.println("【手动刷新】配置刷新完成，刷新的配置项: " + refreshed);
        
        return "配置刷新成功，刷新的配置项: " + refreshed;
    }
    
    /**
     * 查看当前配置
     */
    @RequestMapping("/current")
    public String getCurrentConfig() {
        return String.format(
            "当前配置:\n" +
            "batchDataProcessMode: %s\n" +
            "deferredTriggerStrategy: %s\n" +
            "deleteTempFile: %s\n" +
            "totalCount: %d\n" +
            "pageRowNumber: %d",
            exportProperties.getBatchDataProcessMode(),
            exportProperties.getDeferredTriggerStrategy(),
            exportProperties.isDeleteTempFile(),
            exportProperties.getTotalCount(),
            exportProperties.getPageRowNumber()
        );
    }
}
