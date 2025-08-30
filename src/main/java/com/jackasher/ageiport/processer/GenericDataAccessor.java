package com.jackasher.ageiport.processer;

import com.jackasher.ageiport.model.export.GenericExportQuery;

import java.util.List;

/**
 * 通用数据访问接口
 * 用于抽象不同数据源的访问逻辑
 * 
 * @param <QUERY> 查询参数类型
 * @param <DATA> 数据模型类型
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
public interface GenericDataAccessor<QUERY extends GenericExportQuery, DATA> {
    
    /**
     * 根据查询条件统计总数
     * 
     * @param query 查询条件
     * @return 符合条件的记录总数
     */
    Long countByQuery(QUERY query);
    
    /**
     * 分页查询数据
     * 
     * @param query 查询条件
     * @param offset 偏移量（从0开始）
     * @param size 页大小
     * @return 查询结果列表
     */
    List<DATA> queryByPage(QUERY query, long offset, int size);
}