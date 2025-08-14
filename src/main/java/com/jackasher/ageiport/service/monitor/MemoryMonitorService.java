package com.jackasher.ageiport.service.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.RuntimeMXBean;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存监控服务
 * 定时分析程序的内存占用情况
 * 
 * @author jackasher
 */
@Slf4j
@Service
public class MemoryMonitorService {

    private final MemoryMXBean memoryMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final List<GarbageCollectorMXBean> gcMXBeans;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    
    // 用于统计GC次数和时间的增量
    private final AtomicLong lastGcCount = new AtomicLong(0);
    private final AtomicLong lastGcTime = new AtomicLong(0);

    public MemoryMonitorService() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    /**
     * 定时任务：每30秒执行一次内存分析
     */
    @Scheduled(fixedRate = 10000) // 30秒执行一次
    public void analyzeMemoryUsage() {
        try {
            MemoryAnalysisResult result = getCurrentMemoryInfo();
            logMemoryInfo(result);
            
            // 如果内存使用率超过80%，记录警告
            if (result.getHeapUsagePercent() > 80.0) {
                log.warn("⚠️ 内存使用率过高: {}%, 建议进行GC或检查内存泄漏", 
                    decimalFormat.format(result.getHeapUsagePercent()));
            }
            
            // 如果非堆内存使用率超过90%，记录警告
            if (result.getNonHeapUsagePercent() > 90.0) {
                log.warn("⚠️ 非堆内存使用率过高: {}%, 可能需要调整Metaspace大小", 
                    decimalFormat.format(result.getNonHeapUsagePercent()));
            }
            
        } catch (Exception e) {
            log.error("内存监控分析失败", e);
        }
    }

    /**
     * 获取当前内存信息
     */
    public MemoryAnalysisResult getCurrentMemoryInfo() {
        MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
        
        // 计算GC统计信息
        GcStatistics gcStats = calculateGcStatistics();
        
        return MemoryAnalysisResult.builder()
            .timestamp(System.currentTimeMillis())
            .uptime(runtimeMXBean.getUptime())
            
            // 堆内存信息
            .heapInit(heapMemory.getInit())
            .heapUsed(heapMemory.getUsed())
            .heapCommitted(heapMemory.getCommitted())
            .heapMax(heapMemory.getMax())
            .heapUsagePercent(calculateUsagePercent(heapMemory.getUsed(), heapMemory.getMax()))
            
            // 非堆内存信息  
            .nonHeapInit(nonHeapMemory.getInit())
            .nonHeapUsed(nonHeapMemory.getUsed())
            .nonHeapCommitted(nonHeapMemory.getCommitted())
            .nonHeapMax(nonHeapMemory.getMax())
            .nonHeapUsagePercent(calculateUsagePercent(nonHeapMemory.getUsed(), nonHeapMemory.getMax()))
            
            // GC信息
            .gcCount(gcStats.getTotalGcCount())
            .gcTime(gcStats.getTotalGcTime())
            .gcCountSinceLastCheck(gcStats.getGcCountSinceLastCheck())
            .gcTimeSinceLastCheck(gcStats.getGcTimeSinceLastCheck())
            
            .build();
    }

    /**
     * 计算GC统计信息
     */
    private GcStatistics calculateGcStatistics() {
        long totalGcCount = 0;
        long totalGcTime = 0;
        
        for (GarbageCollectorMXBean gcBean : gcMXBeans) {
            totalGcCount += gcBean.getCollectionCount();
            totalGcTime += gcBean.getCollectionTime();
        }
        
        // 计算自上次检查以来的增量
        long currentLastGcCount = lastGcCount.getAndSet(totalGcCount);
        long currentLastGcTime = lastGcTime.getAndSet(totalGcTime);
        
        long gcCountSinceLastCheck = totalGcCount - currentLastGcCount;
        long gcTimeSinceLastCheck = totalGcTime - currentLastGcTime;
        
        return new GcStatistics(totalGcCount, totalGcTime, gcCountSinceLastCheck, gcTimeSinceLastCheck);
    }

    /**
     * 计算使用率百分比
     */
    private double calculateUsagePercent(long used, long max) {
        if (max <= 0) {
            return 0.0;
        }
        return (double) used / max * 100.0;
    }

    /**
     * 记录内存信息日志
     */
    private void logMemoryInfo(MemoryAnalysisResult result) {
        log.info("📊 内存监控报告 - JVM运行时间: {} 分钟", result.getUptime() / (1000 * 60));
        
        log.info("🏠 堆内存: 已用 {} / 最大 {} ({}%) | 已提交 {}", 
            formatBytes(result.getHeapUsed()),
            formatBytes(result.getHeapMax()),
            decimalFormat.format(result.getHeapUsagePercent()),
            formatBytes(result.getHeapCommitted()));
            
        log.info("📚 非堆内存: 已用 {} / 最大 {} ({}%) | 已提交 {}", 
            formatBytes(result.getNonHeapUsed()),
            result.getNonHeapMax() > 0 ? formatBytes(result.getNonHeapMax()) : "无限制",
            decimalFormat.format(result.getNonHeapUsagePercent()),
            formatBytes(result.getNonHeapCommitted()));
            
        if (result.getGcCountSinceLastCheck() > 0) {
            log.info("🗑️ GC活动: 总次数 {} (+{}), 总时间 {}ms (+{}ms)", 
                result.getGcCount(),
                result.getGcCountSinceLastCheck(),
                result.getGcTime(),
                result.getGcTimeSinceLastCheck());
        } else {
            log.info("🗑️ GC统计: 总次数 {}, 总时间 {}ms (本周期无GC)", 
                result.getGcCount(),
                result.getGcTime());
        }
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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
        
        return decimalFormat.format(size) + " " + units[unitIndex];
    }

    /**
     * GC统计信息内部类
     */
    private static class GcStatistics {
        private final long totalGcCount;
        private final long totalGcTime;
        private final long gcCountSinceLastCheck;
        private final long gcTimeSinceLastCheck;

        public GcStatistics(long totalGcCount, long totalGcTime, long gcCountSinceLastCheck, long gcTimeSinceLastCheck) {
            this.totalGcCount = totalGcCount;
            this.totalGcTime = totalGcTime;
            this.gcCountSinceLastCheck = gcCountSinceLastCheck;
            this.gcTimeSinceLastCheck = gcTimeSinceLastCheck;
        }

        public long getTotalGcCount() { return totalGcCount; }
        public long getTotalGcTime() { return totalGcTime; }
        public long getGcCountSinceLastCheck() { return gcCountSinceLastCheck; }
        public long getGcTimeSinceLastCheck() { return gcTimeSinceLastCheck; }
    }
}
