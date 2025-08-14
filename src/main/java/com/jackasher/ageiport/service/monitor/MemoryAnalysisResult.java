package com.jackasher.ageiport.service.monitor;

import lombok.Builder;
import lombok.Data;

/**
 * 内存分析结果数据模型
 * 
 * @author jackasher
 */
@Data
@Builder
public class MemoryAnalysisResult {
    
    // 基本信息
    private long timestamp;
    private long uptime;
    
    // 堆内存信息
    private long heapInit;
    private long heapUsed;
    private long heapCommitted;
    private long heapMax;
    private double heapUsagePercent;
    
    // 非堆内存信息
    private long nonHeapInit;
    private long nonHeapUsed;
    private long nonHeapCommitted;
    private long nonHeapMax;
    private double nonHeapUsagePercent;
    
    // GC信息
    private long gcCount;
    private long gcTime;
    private long gcCountSinceLastCheck;
    private long gcTimeSinceLastCheck;
}
