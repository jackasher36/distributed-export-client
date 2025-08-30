package com.jackasher.ageiport.processer;

/**
 * 通用数据转换接口
 * 用于将数据模型转换为视图模型
 * 
 * @param <DATA> 数据模型类型
 * @param <VIEW> 视图模型类型
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
public interface GenericDataConverter<DATA, VIEW> {
    
    /**
     * 将数据模型转换为视图模型
     * 
     * @param data 数据模型
     * @return 视图模型
     */
    VIEW convertToView(DATA data);
}