package com.jackasher.ageiport.model.ir_message;

import com.jackasher.ageiport.model.export.ExportParams;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * IR消息查询条件
 * @author Jackasher
 * @version 1.0
 * @className IrMessageQuery
 * @since 1.0
 **/
@Data
public class IrMessageQuery {
    
    private String uuid;
    private Date createdTimeStart;
    private Date createdTimeEnd;
    private String deviceType;
    private String deviceNumber;
    private String archiveName;
    private String bucketName;
    private String areaNumber;
    private String areaName;
    private String fileName;
    private String dataSourceType;
    //默认1000,防止用户不传参,直接导出整张表
    private Integer totalCount;

    //-------------------配置项-------------------------
    //该配置项由用户动态传入,会覆盖原有配置文件的配置
    private ExportParams exportParams;

    /**
     * 防止后续判断时为null
     */
    public IrMessageQuery() {
        this.exportParams = new ExportParams();
    }
}
