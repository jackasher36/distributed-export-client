package com.jackasher.ageiport.controller;

import com.jackasher.ageiport.service.monitor.MemoryAnalysisResult;
import com.jackasher.ageiport.service.monitor.MemoryMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 内存监控控制器
 * 提供内存状态查询接口
 * 
 * @author jackasher
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor/memory")
@RequiredArgsConstructor
public class MemoryMonitorController {

    private final MemoryMonitorService memoryMonitorService;

    /**
     * 获取当前内存状态
     */
    @GetMapping("/status")
    public Map<String, Object> getMemoryStatus() {
        log.info("手动获取内存状态");
        
        MemoryAnalysisResult result = memoryMonitorService.getCurrentMemoryInfo();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());
        response.put("data", result);
        response.put("summary", buildSummary(result));
        
        return response;
    }

    /**
     * 手动触发内存分析
     */
    @PostMapping("/analyze")
    public Map<String, Object> triggerMemoryAnalysis() {
        log.info("手动触发内存分析");
        
        try {
            memoryMonitorService.analyzeMemoryUsage();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "内存分析已完成，请查看日志");
            response.put("timestamp", System.currentTimeMillis());
            
            return response;
        } catch (Exception e) {
            log.error("手动触发内存分析失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "内存分析失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return response;
        }
    }

    /**
     * 获取内存使用简要信息
     */
    @GetMapping("/summary")
    public Map<String, Object> getMemorySummary() {
        MemoryAnalysisResult result = memoryMonitorService.getCurrentMemoryInfo();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", buildSummary(result));
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }

    /**
     * 手动执行垃圾回收（谨慎使用）
     */
    @PostMapping("/gc")
    public Map<String, Object> triggerGarbageCollection() {
        log.warn("手动触发垃圾回收");
        
        try {
            // 获取GC前的内存状态
            MemoryAnalysisResult beforeGc = memoryMonitorService.getCurrentMemoryInfo();
            
            // 建议垃圾回收
            System.gc();
            
            // 等待一小段时间让GC完成
            Thread.sleep(1000);
            
            // 获取GC后的内存状态
            MemoryAnalysisResult afterGc = memoryMonitorService.getCurrentMemoryInfo();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "垃圾回收已触发");
            response.put("beforeGc", buildSummary(beforeGc));
            response.put("afterGc", buildSummary(afterGc));
            response.put("timestamp", System.currentTimeMillis());
            
            return response;
        } catch (Exception e) {
            log.error("手动触发垃圾回收失败", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "垃圾回收触发失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return response;
        }
    }

    /**
     * 构建内存使用简要信息
     */
    private Map<String, Object> buildSummary(MemoryAnalysisResult result) {
        Map<String, Object> summary = new HashMap<>();
        
        // 基本信息
        summary.put("uptime", result.getUptime() / (1000 * 60) + " 分钟");
        
        // 堆内存
        Map<String, Object> heap = new HashMap<>();
        heap.put("used", formatBytes(result.getHeapUsed()));
        heap.put("max", formatBytes(result.getHeapMax()));
        heap.put("usagePercent", String.format("%.2f%%", result.getHeapUsagePercent()));
        heap.put("status", getMemoryStatus(result.getHeapUsagePercent()));
        summary.put("heap", heap);
        
        // 非堆内存
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("used", formatBytes(result.getNonHeapUsed()));
        nonHeap.put("max", result.getNonHeapMax() > 0 ? formatBytes(result.getNonHeapMax()) : "无限制");
        nonHeap.put("usagePercent", String.format("%.2f%%", result.getNonHeapUsagePercent()));
        nonHeap.put("status", getMemoryStatus(result.getNonHeapUsagePercent()));
        summary.put("nonHeap", nonHeap);
        
        // GC信息
        Map<String, Object> gc = new HashMap<>();
        gc.put("totalCount", result.getGcCount());
        gc.put("totalTime", result.getGcTime() + "ms");
        gc.put("recentCount", result.getGcCountSinceLastCheck());
        gc.put("recentTime", result.getGcTimeSinceLastCheck() + "ms");
        summary.put("gc", gc);
        
        return summary;
    }

    /**
     * 根据使用率获取内存状态
     */
    private String getMemoryStatus(double usagePercent) {
        if (usagePercent < 50) {
            return "良好";
        } else if (usagePercent < 70) {
            return "正常";
        } else if (usagePercent < 85) {
            return "偏高";
        } else {
            return "危险";
        }
    }

    /**
     * 格式化字节数为可读格式
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "未知";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024.0 && unitIndex < units.length - 1) {
            size /= 1024.0;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
