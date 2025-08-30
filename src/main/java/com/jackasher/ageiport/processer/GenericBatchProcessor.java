package com.jackasher.ageiport.processer;

import java.util.List;

import com.jackasher.ageiport.model.export.GenericExportQuery;

/**
 * 通用批处理接口
 * 用于抽象不同数据类型的批处理逻辑（如附件处理、数据清洗等）
 * 
 * @param <DATA> 数据模型类型
 * @param <QUERY> 查询参数类型
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
public interface GenericBatchProcessor<DATA, QUERY extends GenericExportQuery> {
    
    /**
     * 处理批量数据
     * 
     * @param dataList 待处理的数据列表
     * @param subTaskId 子任务ID
     * @param pageNum 页码/批次号
     * @param query 查询参数
     * @param mainTaskId 主任务ID
     */
    void processBatchData(List<DATA> dataList, String subTaskId, int pageNum, QUERY query, String mainTaskId);
}