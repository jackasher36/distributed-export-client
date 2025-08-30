package com.jackasher.ageiport.model.user;

import java.io.Serializable;
import java.util.Date;

import com.jackasher.ageiport.model.export.ExportParams;

import com.jackasher.ageiport.model.export.GenericExportQuery;
import lombok.Data;

/**
 * 用户导出查询条件
 * 展示如何为其他数据类型实现通用导出框架
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Data
public class UserQuery implements Serializable, GenericExportQuery {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String email;
    private String department;
    private Date createdTimeStart;
    private Date createdTimeEnd;
    private Integer status; // 0-禁用, 1-启用

    // 导出参数
    private ExportParams exportParams;

    public UserQuery() {
        this.exportParams = new ExportParams();
    }
}