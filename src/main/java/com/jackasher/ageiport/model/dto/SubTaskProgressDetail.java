package com.jackasher.ageiport.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jackasher.ageiport.constant.PostProcessingTaskStatus;
import lombok.Data;

import java.io.Serializable;

@Data
public class SubTaskProgressDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private String mainTaskId;
    private int subTaskNo;
    private String subTaskId;
    private String status = PostProcessingTaskStatus.PENDING.name();
    private long totalItems = 0;
    private long processedItems = 0;
    private long failedItems = 0;
    private long startTime;
    private long finishTime;
    private String resultMessage;

    /**
     * 动态计算该子任务的百分比
     * @return 子任务进度百分比
     */
    @JsonIgnore
    public double getPercent() {
        if (totalItems == 0) {
            return 100.0; // 如果没有条目，视为完成
        }
        double percent = (((double) processedItems + failedItems) / totalItems) * 100.0;
        return Math.min(percent, 100.0);
    }
}