package com.jackasher.ageiport.model.dto;

import java.io.Serializable;
import java.util.List;

import com.jackasher.ageiport.model.export.GenericExportQuery;

import lombok.Data;

// 通用处理上下文 - 支持泛型
@Data
public class ProcessContext<DATA, QUERY extends GenericExportQuery> implements Serializable {
    private static final long serialVersionUID = 1L;
    public final List<DATA> messages;
    public final String subTaskId;
    public final int subTaskNo;
    public final QUERY query;
    public final String mainTaskId;

    public ProcessContext(List<DATA> messages, String subTaskId, int subTaskNo,
                   QUERY query, String mainTaskId) {
        this.messages = messages;
        this.subTaskId = subTaskId;
        this.subTaskNo = subTaskNo;
        this.query = query;
        this.mainTaskId = mainTaskId;
    }
}