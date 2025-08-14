package com.jackasher.ageiport.model.ir_message;

import com.alibaba.ageiport.processor.core.annotation.ViewField;
import lombok.Data;

/**
 * IR消息导出视图
 * @author Jackasher
 * @version 1.0
 * @className IrMessageView
 * @since 1.0
 **/
@Data
public class IrMessageView {

    @ViewField(headerName = "消息ID")
    private String uuid;

    @ViewField(headerName = "创建时间")
    private String createdTime; // 格式化后的时间字符串

    @ViewField(headerName = "设备类型")
    private String deviceType;

    @ViewField(headerName = "设备编号")
    private String deviceNumber;

    @ViewField(headerName = "归档名称")
    private String archiveName;

    @ViewField(headerName = "存储桶名称")
    private String bucketName;

    @ViewField(headerName = "区域编号")
    private String areaNumber;

    @ViewField(headerName = "区域名称")
    private String areaName;

    @ViewField(headerName = "文件名称")
    private String fileName;

    @ViewField(headerName = "文件大小")
    private Integer fileLength;

    @ViewField(headerName = "Die挑选文件名")
    private String diePickingFileName;

    @ViewField(headerName = "DDC文件名")
    private String ddcFileName;

    @ViewField(headerName = "解调文件名")
    private String demodulationFileName;

    @ViewField(headerName = "解码文件名")
    private String decodeFileName;

    @ViewField(headerName = "解码前文件名")
    private String beforeDecodeFileName;

    @ViewField(headerName = "OBT上行文件名")
    private String obtUlFileName;

    @ViewField(headerName = "OBT下行文件名")
    private String obtDlFileName;

    @ViewField(headerName = "数据源类型")
    private String dataSourceType;


}
