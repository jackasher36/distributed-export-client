package com.jackasher.ageiport.model.export;

/**
 * 通用导出查询接口
 * 所有导出查询类都应该实现此接口
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
public interface GenericExportQuery {
    
    /**
     * 获取导出参数
     * @return 导出参数
     */
    ExportParams getExportParams();
    
    /**
     * 设置导出参数
     * @param exportParams 导出参数
     */
    void setExportParams(ExportParams exportParams);
}