package com.jackasher.ageiport.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jackasher.ageiport.constant.PostProcessingTaskStatus;
import lombok.Data;
import java.io.Serializable;

@Data
public class ProgressSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 总共需要处理的子任务（批次）数量。
     */
    private long totalSubTasks = 0;

    /**
     * 已经完成（无论成功或失败）的子任务数量。
     */
    private long completedSubTasks = 0;


    /**
     * 【可选】所有批次处理完后，最终统计出的附件总数。
     * 这个字段现在仅用于最终展示，不再用于进度计算。
     */
    private long totalItems = 0;

    /**
     * 【可选】所有批次处理完后，最终统计出的成功附件总数。
     */
    private long processedItems = 0;

    /**
     * 【可选】所有批次处理完后，最终统计出的失败附件总数。
     */
    private long failedItems = 0;

    private String status = PostProcessingTaskStatus.PENDING.name();

    /**
     * 动态计算总体进度百分比，基于已完成的子任务数量。
     * @return 总体进度百分比
     */
    @JsonIgnore
    public double getPercent() {
        // 如果总子任务数为0（例如，导出任务本身没有数据），则进度为0
        if (totalSubTasks == 0) {
            // 除非状态已经是完成，比如被标记为SKIPPED
            if (PostProcessingTaskStatus.COMPLETED.name().equals(status) ||
                    PostProcessingTaskStatus.SKIPPED.name().equals(status)) {
                return 100.0;
            }
            return 0.0;
        }

        // 核心计算公式：(已完成的子任务数 / 总子任务数) * 100
        double percent = (((double) completedSubTasks) / totalSubTasks) * 100.0;

        return Math.min(percent, 100.0);
    }
}