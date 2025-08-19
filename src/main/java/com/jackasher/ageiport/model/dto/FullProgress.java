package com.jackasher.ageiport.model.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 封装完整的附件处理进度，包含宏观总结和所有子任务的详细列表。
 * 这个对象将作为API的直接返回类型，提供给Controller层使用。
 *
 * @author Jackasher
 */
@Data
public class FullProgress implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 附件处理的宏观进度总结。
     */
    private ProgressSummary summary;

    /**
     * 所有子任务（批次）的详细进度列表。
     * 列表将按 subTaskNo 排序。
     */
    private List<SubTaskProgressDetail> subTasks;
}