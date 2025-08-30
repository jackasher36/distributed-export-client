package com.jackasher.ageiport.processer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.ageiport.processor.core.exception.BizException;
import com.alibaba.ageiport.processor.core.file.excel.ExcelConstants;
import com.alibaba.ageiport.processor.core.model.api.BizColumnHeader;
import com.alibaba.ageiport.processor.core.model.api.BizColumnHeaders;
import com.alibaba.ageiport.processor.core.model.api.BizData;
import com.alibaba.ageiport.processor.core.model.api.BizDataGroup;
import com.alibaba.ageiport.processor.core.model.api.BizDataItem;
import com.alibaba.ageiport.processor.core.model.api.BizExportPage;
import com.alibaba.ageiport.processor.core.model.api.BizUser;
import com.alibaba.ageiport.processor.core.model.api.impl.BizColumnHeaderImpl;
import com.alibaba.ageiport.processor.core.model.api.impl.BizColumnHeadersImpl;
import com.alibaba.ageiport.processor.core.model.api.impl.BizDataGroupImpl;
import com.alibaba.ageiport.processor.core.model.core.ColumnHeader;
import com.alibaba.ageiport.processor.core.model.core.ColumnHeaders;
import com.alibaba.ageiport.processor.core.task.exporter.ExportProcessor;
import com.alibaba.ageiport.processor.core.task.exporter.api.BizExportTaskRuntimeConfig;
import com.alibaba.ageiport.processor.core.task.exporter.api.BizExportTaskRuntimeConfigImpl;
import com.alibaba.ageiport.processor.core.task.exporter.context.ExportMainTaskContext;
import com.alibaba.ageiport.processor.core.task.exporter.context.ExportSubTaskContext;
import com.alibaba.ageiport.processor.core.utils.HeadersUtil;
import com.jackasher.ageiport.model.export.ExportParams;
import com.jackasher.ageiport.model.export.GenericExportQuery;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;

/**
 * 通用数据导出处理器基类
 * 提供了完整的导出流程框架，子类只需要实现具体的数据操作接口
 * 
 * @param <QUERY> 查询参数类型
 * @param <DATA> 数据模型类型  
 * @param <VIEW> 视图模型类型
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
public abstract class GenericExportAdapter<QUERY extends GenericExportQuery, DATA, VIEW>
        implements ExportProcessor<QUERY, DATA, VIEW> {

    private static final Logger log = LoggerFactory.getLogger(GenericExportAdapter.class);

    /**
     * 获取数据访问器，子类需要实现此方法来提供具体的数据操作
     */
    protected abstract GenericDataAccessor<QUERY, DATA> getDataAccessor();

    /**
     * 获取数据转换器，子类需要实现此方法来提供数据到视图的转换
     */
    protected abstract GenericDataConverter<DATA, VIEW> getDataConverter();

    /**
     * 获取批处理器，子类需要实现此方法来提供批数据处理逻辑
     */
    protected abstract GenericBatchProcessor<DATA, QUERY> getBatchProcessor();

    /**
     * 获取视图类的Class对象，用于生成表头
     */
    protected abstract Class<VIEW> getViewClass();

    /**
     * 获取导出规格代码，用于标识不同的导出类型
     */
    protected abstract String getExportCode();

    /**
     * [生命周期-1: 主任务节点] 配置任务运行时参数
     */
    @Override
    public BizExportTaskRuntimeConfig taskRuntimeConfig(BizUser user, QUERY query) throws BizException {
        log.info("[LIFECYCLE-MAIN-1] taskRuntimeConfig: 开始为{}配置运行时参数...", getExportCode());

        BizExportTaskRuntimeConfigImpl config = new BizExportTaskRuntimeConfigImpl();

        // 获取分页大小配置
        int configPageSize = SpringContextUtil.exportProperties().getPageRowNumber();
        log.debug("[LIFECYCLE-MAIN-1] taskRuntimeConfig: 从配置文件加载到默认pageSize: {}", configPageSize);

        // 分片大小优先级：查询参数 > 配置文件 > 默认值
        Integer pageRowNumber = resolvePageRowNumber(query, configPageSize);
        log.info("[LIFECYCLE-MAIN-1] taskRuntimeConfig: 最终确定分片大小(pageSize)为: {}", pageRowNumber);

        config.setPageSize(pageRowNumber);
        return config;
    }

    /**
     * [生命周期-2: 主任务节点] 查询总数据量
     */
    @Override
    public Integer totalCount(BizUser bizUser, QUERY query) throws BizException {
        log.info("[LIFECYCLE-MAIN-2] totalCount: 开始统计{}的总数据量", getExportCode());

        try {
            // 使用数据访问器查询总数
            Long count = getDataAccessor().countByQuery(query);
            int totalCountInDB = count.intValue();
            log.info("[LIFECYCLE-MAIN-2] totalCount: 数据库中符合条件的记录总数为: {}", totalCountInDB);

            // 获取配置文件中定义的最大导出数量
            int configTotalCount = SpringContextUtil.exportProperties().getTotalCount();
            log.debug("[LIFECYCLE-MAIN-2] totalCount: 从配置文件加载到最大导出限制: {}", configTotalCount);

            // 最大导出量优先级：查询参数 > 配置文件
            int maxTotalCount = resolveTotalCount(query, configTotalCount);

            int finalTotalCount = Math.min(totalCountInDB, maxTotalCount);
            log.info("[LIFECYCLE-MAIN-2] totalCount: 最终确定要导出的总数据量为: {}", finalTotalCount);

            return finalTotalCount;
        } catch (Exception e) {
            log.error("[LIFECYCLE-MAIN-2] totalCount: 查询{}总数时发生数据库异常", getExportCode(), e);
            throw new BizException("QUERY_TOTAL_COUNT_ERROR", "查询" + getExportCode() + "总数失败: " + e.getMessage());
        }
    }

    /**
     * [生命周期-3: 主任务节点] 动态构建表头
     */
    @Override
    public BizColumnHeaders getHeaders(BizUser user, QUERY query) throws BizException {
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 开始为{}动态构建表头...", getExportCode());
        ExportMainTaskContext<?, ?, ?> context = (ExportMainTaskContext<?, ?, ?>) getContext();

        // 获取totalCount方法计算出的总数
        int totalCount = context.getExportTaskRuntimeConfig().getTotalCount();
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 获取到任务总数为: {}", totalCount);

        // 如果 totalCount 为 0，也至少创建一个 Sheet 的表头
        if (totalCount == 0) {
            totalCount = 1;
        }

        // 计算总共会产生多少个Sheet
        int configSheetRowNumber = SpringContextUtil.exportProperties().getSheetRowNumber();
        Integer sheetRowNumber = resolveSheetRowNumber(query, configSheetRowNumber);

        int totalSheets = (totalCount - 1) / sheetRowNumber + 1;
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 根据总数 {} 和单Sheet行数 {}, 计算出将生成 {} 个 Sheet", 
                totalCount, sheetRowNumber, totalSheets);

        // 使用框架工具类从视图类生成基础表头模板
        ColumnHeaders baseColumnHeaders = HeadersUtil.buildHeaders(null, getViewClass(), null);
        log.debug("[LIFECYCLE-MAIN-3] getHeaders: 从 {} 解析出 {} 个基础表头", 
                getViewClass().getSimpleName(), baseColumnHeaders.getColumnHeaders().size());

        BizColumnHeadersImpl bizColumnHeaders = new BizColumnHeadersImpl();
        List<BizColumnHeader> bizHeaders = new ArrayList<>();
        bizColumnHeaders.setBizColumnHeaders(bizHeaders);

        // 为所有Sheet定义一套通用的表头（groupIndex = -1）
        for (ColumnHeader baseHeader : baseColumnHeaders.getColumnHeaders()) {
            BizColumnHeaderImpl newHeader = new BizColumnHeaderImpl();
            newHeader.setHeaderName(baseHeader.getHeaderName());
            newHeader.setFieldName(baseHeader.getFieldName());
            newHeader.setDataType(baseHeader.getType());
            newHeader.setColumnWidth(baseHeader.getColumnWidth());
            newHeader.setRequired(baseHeader.getRequired());
            newHeader.setErrorHeader(baseHeader.getErrorHeader());
            newHeader.setGroupIndex(-1); // -1表示此表头适用于所有Sheet
            bizHeaders.add(newHeader);
        }
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 已创建 {} 个通用表头 (groupIndex=-1)", bizHeaders.size());

        // 为每个Sheet创建虚拟表头以确保Sheet的创建
        for (int i = 0; i < totalSheets; i++) {
            BizColumnHeaderImpl dummyHeader = new BizColumnHeaderImpl();
            dummyHeader.setFieldName("dummy_field_for_group_" + i);
            dummyHeader.setHeaderName(Collections.singletonList("DUMMY"));
            dummyHeader.setGroupIndex(i);
            bizHeaders.add(dummyHeader);
        }
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 已为 {} 个Sheet添加了虚拟表头", totalSheets);
        return bizColumnHeaders;
    }

    /**
     * [生命周期-4: 子任务节点] 查询分片数据
     */
    @Override
    public List<DATA> queryData(BizUser user, QUERY query, BizExportPage bizExportPage) throws BizException {
        ExportSubTaskContext<?, ?, ?> context = (ExportSubTaskContext<?, ?, ?>) getContext();
        String subTaskId = context != null ? context.getSubTask().getSubTaskId() : "UNKNOWN";
        log.info("[LIFECYCLE-SUB-1] queryData on subTask: {}: 开始查询{}数据分片，Offset: {}, Size: {}", 
                subTaskId, getExportCode(), bizExportPage.getOffset(), bizExportPage.getSize());

        // 获取最大导出量限制
        Integer maxTotalCount = resolveTotalCount(query, SpringContextUtil.exportProperties().getTotalCount());

        int pageSize = bizExportPage.getSize();
        // 如果当前分片的结束位置超过了总数限制，则调整页大小
        if (bizExportPage.getOffset() + pageSize > maxTotalCount) {
            pageSize = maxTotalCount - bizExportPage.getOffset();
            log.warn("[LIFECYCLE-SUB-1] queryData on subTask: {}: 分片大小调整，原始Size: {}, 调整后Size: {}", 
                    subTaskId, bizExportPage.getSize(), pageSize);
        }
        if (pageSize <= 0) {
            log.info("[LIFECYCLE-SUB-1] queryData on subTask: {}: 计算后的pageSize为0，无需查询，返回空列表。", subTaskId);
            return Collections.emptyList();
        }

        try {
            // 使用数据访问器查询数据
            List<DATA> dataList = getDataAccessor().queryByPage(query, bizExportPage.getOffset(), pageSize);
            log.info("[LIFECYCLE-SUB-1] queryData on subTask: {}: 成功查询到 {} 条数据", subTaskId, dataList.size());
            return dataList;

        } catch (Exception e) {
            log.error("[LIFECYCLE-SUB-1] queryData on subTask: {}: 查询{}数据时发生数据库异常", subTaskId, getExportCode(), e);
            throw new BizException("QUERY_DATA_ERROR", "查询" + getExportCode() + "数据失败: " + e.getMessage());
        }
    }

    /**
     * [生命周期-5: 子任务节点] 数据转换和批处理
     */
    @Override
    public List<VIEW> convert(BizUser user, QUERY query, List<DATA> dataList) throws BizException {
        ExportSubTaskContext<?, ?, ?> context = (ExportSubTaskContext<?, ?, ?>) getContext();
        String subTaskId = context.getSubTask().getSubTaskId();
        int subTaskNo = context.getSubTask().getSubTaskNo();
        log.info("[LIFECYCLE-SUB-2] convert on subTask: {}: 开始处理{}批次 #{} 的数据...", subTaskId, getExportCode(), subTaskNo);

        // 1. 处理批数据（支持多种模式：同步/异步/延迟/跳过）
        try {
            String mainTaskId = context.getMainTask().getMainTaskId();
            getBatchProcessor().processBatchData(dataList, subTaskId, subTaskNo, query, mainTaskId);
        } catch (Exception e) {
            // 只记录日志，不中断导出流程
            log.error("子任务 {} 在处理{}批数据时发生错误，但导出将继续。错误: {}", subTaskId, getExportCode(), e.getMessage(), e);
        }

        // 2. 执行数据模型转换，生成用于Excel的View列表
        List<VIEW> viewList = dataList.stream()
                .map(getDataConverter()::convertToView)
                .collect(Collectors.toList());

        log.info("[LIFECYCLE-SUB-2] convert on subTask: {}: {}数据转换完成。", subTaskId, getExportCode());
        return viewList;
    }

    /**
     * [生命周期-6: 子任务节点] 数据分组到不同Sheet
     */
    @Override
    public BizDataGroup<VIEW> group(BizUser user, QUERY query, List<VIEW> viewList) {
        ExportSubTaskContext<?, ?, ?> context = (ExportSubTaskContext<?, ?, ?>) getContext();
        String subTaskId = context != null && context.getSubTask() != null ? context.getSubTask().getSubTaskId() : "UNKNOWN";
        log.info("[LIFECYCLE-SUB-3] group on subTask: {}: 开始对{}的 {} 条视图数据进行分组...", 
                subTaskId, getExportCode(), viewList.size());

        BizDataGroupImpl<VIEW> bizDataGroup = new BizDataGroupImpl<>();
        List<BizData<VIEW>> sheetDataList = new ArrayList<>();
        bizDataGroup.setData(sheetDataList);

        if (viewList == null || viewList.isEmpty()) {
            log.info("[LIFECYCLE-SUB-3] group on subTask: {}: 视图数据为空，返回空的DataGroup", subTaskId);
            return bizDataGroup;
        }

        // 从配置中获取每个Sheet的大小
        int sheetSize = resolveSheetRowNumber(query, SpringContextUtil.exportProperties().getSheetRowNumber());

        if (context == null) {
            log.warn("[LIFECYCLE-SUB-3] group on subTask: {}: 无法获取子任务上下文，将执行默认的单Sheet分组逻辑", subTaskId);
            return defaultGroup(viewList);
        }

        int subTaskNo = context.getSubTask().getSubTaskNo();
        int pageSize = context.getExportTaskRuntimeConfig().getPageSize();
        // 计算当前子任务数据在全局数据集中的起始偏移量
        long logicalOffset = (long) (subTaskNo - 1) * pageSize;
        log.debug("[LIFECYCLE-SUB-3] group on subTask: {}: 子任务编号:{}, PageSize:{}, 计算出逻辑偏移量:{}", 
                subTaskId, subTaskNo, pageSize, logicalOffset);

        int currentDataIndex = 0;
        while (currentDataIndex < viewList.size()) {
            // 计算当前数据点在全局的逻辑位置，并确定它属于哪个Sheet
            int currentGlobalSheetIndex = (int) ((logicalOffset + currentDataIndex) / sheetSize);
            // 计算当前全局Sheet的结束位置
            long endOfCurrentSheet = (long)(currentGlobalSheetIndex + 1) * sheetSize;
            // 计算当前Sheet还剩下多少空间
            long remainingSpaceInSheet = endOfCurrentSheet - (logicalOffset + currentDataIndex);
            // 本次要写入当前Sheet的数据量
            int chunkSize = (int) Math.min(remainingSpaceInSheet, viewList.size() - currentDataIndex);
            if (chunkSize <= 0) {
                log.warn("[LIFECYCLE-SUB-3] group on subTask: {}: 计算出的 chunkSize 小于等于 0，跳过此次循环", subTaskId);
                currentDataIndex++;
                continue;
            }

            log.debug("[LIFECYCLE-SUB-3] group on subTask: {}: 正在处理全局Sheet: {}, 逻辑偏移量:{}, 本次处理块大小:{}",
                    subTaskId, currentGlobalSheetIndex, logicalOffset + currentDataIndex, chunkSize);

            List<VIEW> viewsForSheet = viewList.subList(currentDataIndex, currentDataIndex + chunkSize);

            // 查找或创建对应的Sheet数据容器
            BizDataGroupImpl.Data<VIEW> currentSheetData = findOrCreateSheetData(sheetDataList, currentGlobalSheetIndex);

            // 将数据块添加到Sheet中
            for (VIEW view : viewsForSheet) {
                BizDataGroupImpl.Item<VIEW> item = new BizDataGroupImpl.Item<>();
                item.setData(view);
                currentSheetData.getItems().add(item);
            }
            // 移动指针
            currentDataIndex += chunkSize;
        }
        log.info("[LIFECYCLE-SUB-3] group on subTask: {}: {}分组完成，共生成/填充了 {} 个Sheet的数据", 
                subTaskId, getExportCode(), sheetDataList.size());
        return bizDataGroup;
    }

    /**
     * 在已有的Sheet数据列表中查找指定索引的Sheet，如果不存在则创建并添加
     */
    private BizDataGroupImpl.Data<VIEW> findOrCreateSheetData(List<BizData<VIEW>> sheetDataList, int sheetIndex) {
        String sheetNoStr = String.valueOf(sheetIndex);
        // 尝试查找已存在的Sheet
        for (BizData<VIEW> sheetData : sheetDataList) {
            if (sheetNoStr.equals(sheetData.getMeta().get(ExcelConstants.sheetNoKey))) {
                return (BizDataGroupImpl.Data<VIEW>) sheetData;
            }
        }
        // 如果不存在，则创建一个新的
        BizDataGroupImpl.Data<VIEW> newSheetData = new BizDataGroupImpl.Data<>();
        String sheetName = getExportCode() + "数据Sheet-" + (sheetIndex + 1);
        Map<String, String> meta = new HashMap<>();
        meta.put(ExcelConstants.sheetNameKey, sheetName);
        meta.put(ExcelConstants.sheetNoKey, sheetNoStr);
        newSheetData.setMeta(meta);
        newSheetData.setItems(new ArrayList<>());
        sheetDataList.add(newSheetData);
        return newSheetData;
    }

    /**
     * 备用的简单group实现
     */
    private BizDataGroup<VIEW> defaultGroup(List<VIEW> views) {
        BizDataGroupImpl<VIEW> group = new BizDataGroupImpl<>();
        BizDataGroupImpl.Data<VIEW> data = new BizDataGroupImpl.Data<>();
        List<BizData<VIEW>> dataList = new ArrayList<>();
        dataList.add(data);
        group.setData(dataList);

        Map<String, String> meta = new HashMap<>();
        meta.put(ExcelConstants.sheetNameKey, getExportCode() + "Sheet1");
        meta.put(ExcelConstants.sheetNoKey, "0");
        data.setMeta(meta);

        List<BizDataItem<VIEW>> items = new ArrayList<>();
        data.setItems(items);
        for (VIEW view : views) {
            BizDataGroupImpl.Item<VIEW> item = new BizDataGroupImpl.Item<>();
            item.setData(view);
            items.add(item);
        }
        return group;
    }

    // 以下是配置解析的辅助方法，子类可以覆盖以实现自定义逻辑
    /**
     * 解析页大小配置
     */
    protected Integer resolvePageRowNumber(QUERY query, int defaultValue) {
        ExportParams exportParams = query.getExportParams();
        return Optional.ofNullable(exportParams != null ? exportParams.getPageRowNumber() : null)
                .filter(pn -> pn > 0)
                .orElse(defaultValue);
    }

    /**
     * 解析总数限制配置
     */
    protected Integer resolveTotalCount(QUERY query, int defaultValue) {
        ExportParams exportParams = query.getExportParams();
        return Optional.ofNullable(exportParams != null ? exportParams.getTotalCount() : null)
                .filter(tc -> tc > 0)
                .orElse(defaultValue);
    }

    /**
     * 解析Sheet行数配置
     */
    protected Integer resolveSheetRowNumber(QUERY query, int defaultValue) {
        ExportParams exportParams = query.getExportParams();
        return Optional.ofNullable(exportParams != null ? exportParams.getSheetRowNumber() : null)
                .filter(sn -> sn > 0)
                .orElse(defaultValue);
    }
}