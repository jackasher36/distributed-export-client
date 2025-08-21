package com.jackasher.ageiport.utils.business;

import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jackasher.ageiport.annotation.Timing;
import com.jackasher.ageiport.model.export.ExportParams;
import com.jackasher.ageiport.model.export.FilePaths;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.model.ir_message.IrMessageView;
import com.jackasher.ageiport.model.pojo.IrMessage;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;
import com.jackasher.ageiport.utils.params.reflect.ExportConfigResolver;

/**
 * @author Jackasher
 * 导出处理器的工具类
 * @version 1.0
 * @className IrMessageUtils
 * @since 1.0
 **/
public class IrMessageUtils {
    private static final Logger log = LoggerFactory.getLogger(IrMessageUtils.class);

    private final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * 构建查询条件
     */
    public static LambdaQueryWrapper<IrMessage> irMessageQueryToirMessage(IrMessageQuery query) {
        return irMessageQueryToirMessage(query, true);
    }

    /**
     * 构建查询条件，支持控制是否追加排序（用于 COUNT 查询时禁用 ORDER BY，兼容 StarRocks/分析型数据库限制）
     *
     * @param query 查询参数
     * @param includeOrder 是否包含排序（true: 追加按创建时间倒序；false: 不追加排序）
     */
    public static LambdaQueryWrapper<IrMessage> irMessageQueryToirMessage(IrMessageQuery query, boolean includeOrder) {
        if (query == null) {
            return new LambdaQueryWrapper<>();
        }
        LambdaQueryWrapper<IrMessage> queryWrapper = new LambdaQueryWrapper<>();

        // 根据UUID精确查询
        if (query.getUuid() != null && !query.getUuid().trim().isEmpty()) {
            queryWrapper.eq(IrMessage::getUuid, query.getUuid());
        }

        // 根据创建时间范围查询
        if (query.getCreatedTimeStart() != null) {
            queryWrapper.ge(IrMessage::getCreatedTime, query.getCreatedTimeStart());
        }
        if (query.getCreatedTimeEnd() != null) {
            queryWrapper.le(IrMessage::getCreatedTime, query.getCreatedTimeEnd());
        }

        // 根据设备类型模糊查询
        if (query.getDeviceType() != null && !query.getDeviceType().trim().isEmpty()) {
            queryWrapper.like(IrMessage::getDeviceType, query.getDeviceType());
        }

        // 根据设备编号模糊查询
        if (query.getDeviceNumber() != null && !query.getDeviceNumber().trim().isEmpty()) {
            queryWrapper.like(IrMessage::getDeviceNumber, query.getDeviceNumber());
        }

        // 根据归档名称模糊查询
        if (query.getArchiveName() != null && !query.getArchiveName().trim().isEmpty()) {
            queryWrapper.like(IrMessage::getArchiveName, query.getArchiveName());
        }

        // 根据存储桶名称精确查询
        if (query.getBucketName() != null && !query.getBucketName().trim().isEmpty()) {
            queryWrapper.eq(IrMessage::getBucketName, query.getBucketName());
        }

        // 根据区域编号精确查询
        if (query.getAreaNumber() != null && !query.getAreaNumber().trim().isEmpty()) {
            queryWrapper.eq(IrMessage::getAreaNumber, query.getAreaNumber());
        }

        // 根据区域名称模糊查询
        if (query.getAreaName() != null && !query.getAreaName().trim().isEmpty()) {
            queryWrapper.like(IrMessage::getAreaName, query.getAreaName());
        }

        // 根据文件名模糊查询
        if (query.getFileName() != null && !query.getFileName().trim().isEmpty()) {
            queryWrapper.like(IrMessage::getFileName, query.getFileName());
        }

        // 根据数据源类型精确查询
        if (query.getDataSourceType() != null && !query.getDataSourceType().trim().isEmpty()) {
            queryWrapper.eq(IrMessage::getDataSourceType, query.getDataSourceType());
        }

        // 是否追加排序
        if (includeOrder) {
            queryWrapper.orderByDesc(IrMessage::getCreatedTime);
        }

        return queryWrapper;
    }


    /**
     * 将IrMessage转换为IrMessageData
     */
    public static IrMessageData convertToIrMessageData(IrMessage irMessage) {
        IrMessageData data = new IrMessageData();

        data.setUuid(irMessage.getUuid());
        data.setCreatedTime(irMessage.getCreatedTime());
        data.setDeviceType(irMessage.getDeviceType());
        data.setDeviceNumber(irMessage.getDeviceNumber());
        data.setArchiveName(irMessage.getArchiveName());
        data.setBucketName(irMessage.getBucketName());
        data.setAreaNumber(irMessage.getAreaNumber());
        data.setAreaName(irMessage.getAreaName());
        data.setFileName(irMessage.getFileName());
        data.setFileLength(irMessage.getFileLength());
        data.setDiePickingFileName(irMessage.getDiePickingFileName());
        data.setDdcFileName(irMessage.getDdcFileName());
        data.setDemodulationFileName(irMessage.getDemodulationFileName());
        data.setDecodeFileName(irMessage.getDecodeFileName());
        data.setBeforeDecodeFileName(irMessage.getBeforeDecodeFileName());
        data.setObtUlFileName(irMessage.getObtUlFileName());
        data.setObtDlFileName(irMessage.getObtDlFileName());
        data.setDataSourceType(irMessage.getDataSourceType());

        return data;
    }


    /**
     * 辅助方法：将 IrMessageData 的基础字段映射到 IrMessageView
     */
    public static IrMessageView createViewFromData(IrMessageData data) {
        IrMessageView view = new IrMessageView();
        view.setUuid(data.getUuid());
        view.setDeviceType(data.getDeviceType());
        view.setDeviceNumber(data.getDeviceNumber());
        view.setArchiveName(data.getArchiveName());
        view.setBucketName(data.getBucketName());
        view.setAreaNumber(data.getAreaNumber());
        view.setAreaName(data.getAreaName());
        view.setFileName(data.getFileName());
        view.setFileLength(data.getFileLength());
        view.setDiePickingFileName(data.getDiePickingFileName());
        view.setDdcFileName(data.getDdcFileName());
        view.setDemodulationFileName(data.getDemodulationFileName());
        view.setDecodeFileName(data.getDecodeFileName());
        view.setBeforeDecodeFileName(data.getBeforeDecodeFileName());
        view.setObtUlFileName(data.getObtUlFileName());
        view.setObtDlFileName(data.getObtDlFileName());
        view.setDataSourceType(data.getDataSourceType());
        if (data.getCreatedTime() != null) {
            try {
                view.setCreatedTime(dateFormat.format(data.getCreatedTime()));
            } catch (Exception e) {
                log.warn("格式化日期失败 for UUID: {}", data.getUuid());
                view.setCreatedTime("N/A");
            }
        }
        return view;
    }

    /**
     * 解析参数
     */
        public static ExportParams getResolvedParams(IrMessageQuery query) {
        ExportConfigResolver resolver = SpringContextUtil.getBean(ExportConfigResolver.class);
        return resolver.resolve(query.getExportParams());
    }
    
    /**
     * 构建文件路径
     */
    public static FilePaths buildFilePaths(String subTaskId, IrMessageQuery irMessageQuery) {
        ExportParams resolvedParams = getResolvedParams(irMessageQuery);
        
        String excelFileName = resolvedParams.getExcelFileName() + subTaskId;
        String excelDirectory = resolvedParams.getExcelFileDirectory() + subTaskId;
        String outZipFileName = excelFileName + "_" + subTaskId + "_files.zip";
        String beforeDecodeZipFileName = excelFileName + "_" + subTaskId + "_before_decode_files.zip";
        
        return new FilePaths(excelFileName, excelDirectory, outZipFileName, beforeDecodeZipFileName);
    }

}
