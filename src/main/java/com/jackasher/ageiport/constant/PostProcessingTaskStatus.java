// src/main/java/com/jackasher/ageiport/constant/PostProcessingTaskStatus.java
package com.jackasher.ageiport.constant;

public enum PostProcessingTaskStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    SKIPPED, // 用于明确标记为跳过的任务
    PARTIALLY_COMPLETED // 表示任务完成但有部分失败项
}