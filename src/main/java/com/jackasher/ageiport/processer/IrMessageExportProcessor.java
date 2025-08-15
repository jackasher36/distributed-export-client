package com.jackasher.ageiport.processer;

import com.alibaba.ageiport.processor.core.annotation.ExportSpecification;
import com.alibaba.ageiport.processor.core.constants.ExecuteType;
import com.alibaba.ageiport.processor.core.exception.BizException;
import com.alibaba.ageiport.processor.core.file.excel.ExcelConstants;
import com.alibaba.ageiport.processor.core.model.api.*;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.model.ir_message.IrMessageView;
import com.jackasher.ageiport.service.attachment_service.AttachmentProcessingService;
import com.jackasher.ageiport.utils.AttachmentProcessUtil;
import com.jackasher.ageiport.utils.IrMessageUtils;
import com.jackasher.ageiport.utils.SpringContextUtil;
import com.jackasher.ageiport.model.pojo.IrMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.jackasher.ageiport.utils.IrMessageUtils.*;
import static com.jackasher.ageiport.utils.IrMessageUtils.getResolvedParams;

/**
 * IR消息导出处理器,每个要处理的表,都需要实现一个处理器
 *
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 **/
@ExportSpecification(
        code = "IrMessageExportProcessor",
        name = "IrMessageExportProcessor",
        executeType = ExecuteType.CLUSTER // 指定为集群执行模式
)
public class IrMessageExportProcessor implements ExportProcessor<IrMessageQuery, IrMessageData, IrMessageView> {

    private static final Logger log = LoggerFactory.getLogger(IrMessageExportProcessor.class);



    /**
     * [生命周期-1: 主任务节点]
     * 定义任务运行时的动态配置。此方法在任务开始时于主任务节点上被调用一次。
     * 核心职责是确定数据分片的大小（即每个子任务处理多少数据）。
     *
     * @param user           当前操作的用户信息
     * @param irMessageQuery 前端传入的查询参数
     * @return 业务运行时配置，主要用于设定分片大小(pageSize)
     * @throws BizException 业务异常
     */
    @Override
    public BizExportTaskRuntimeConfig taskRuntimeConfig(BizUser user, IrMessageQuery irMessageQuery) throws BizException {
        log.info("[LIFECYCLE-MAIN-1] taskRuntimeConfig: 开始为任务配置运行时参数...");

        BizExportTaskRuntimeConfigImpl config = new BizExportTaskRuntimeConfigImpl();

        // 默认使用application.yml配置文件中的分片大小
        int configPageSize = SpringContextUtil.exportProperties().getPageRowNumber();
        log.debug("[LIFECYCLE-MAIN-1] taskRuntimeConfig: 从配置文件加载到默认pageSize: {}", configPageSize);

        // 分片大小优先级：API参数 > 配置文件 > 默认值
//        Integer pageSize = Optional.ofNullable(irMessageQuery.getExportParams().getPageRowNumber())
//                .filter(pn -> pn > 0)
//                .orElse(configPageSize);
        Integer pageRowNumber = getResolvedParams(irMessageQuery).getPageRowNumber();
        log.info("[LIFECYCLE-MAIN-1] taskRuntimeConfig: 最终确定分片大小(pageSize)为: {}", pageRowNumber);

        config.setPageSize(pageRowNumber);
        return config;
    }

    /**
     * [生命周期-2: 主任务节点]
     * 查询需要导出的总数据量。此方法在主任务节点上被调用一次，在 taskRuntimeConfig 之后，getHeaders 之前。
     * 它的返回值将决定AGEIPort框架创建多少个子任务。
     *
     * @param bizUser        当前操作的用户信息
     * @param irMessageQuery 前端传入的查询参数
     * @return 符合条件的总记录数
     * @throws BizException 业务异常
     */
    @Override
    public Integer totalCount(BizUser bizUser, IrMessageQuery irMessageQuery) throws BizException {
        log.info("[LIFECYCLE-MAIN-2] totalCount: 开始统计总数据量，查询参数: {}", irMessageQuery);

        try {
            LambdaQueryWrapper<IrMessage> queryWrapper = irMessageQueryToirMessage(irMessageQuery, false);
            Long count = SpringContextUtil.getIrMessageMapper().selectCount(queryWrapper);
            int totalCountInDB = count.intValue();
            log.info("[LIFECYCLE-MAIN-2] totalCount: 数据库中符合条件的记录总数为: {}", totalCountInDB);

            // 获取配置文件中定义的最大导出数量
            int configTotalCount = SpringContextUtil.exportProperties().getTotalCount();
            log.debug("[LIFECYCLE-MAIN-2] totalCount: 从配置文件加载到最大导出限制: {}", configTotalCount);

            // 最大导出量优先级：API参数 > 配置文件
            int maxTotalCount = Optional.ofNullable(irMessageQuery.getExportParams().getTotalCount())
                    .filter(queryTotalCount -> queryTotalCount > 0)
                    .orElse(configTotalCount);

            int finalTotalCount = Math.min(totalCountInDB, maxTotalCount);
            log.info("[LIFECYCLE-MAIN-2] totalCount: 最终确定要导出的总数据量为: {}", finalTotalCount);

            return finalTotalCount;
        } catch (Exception e) {
            log.error("[LIFECYCLE-MAIN-2] totalCount: 查询总数时发生数据库异常", e);
            throw new BizException("QUERY_TOTAL_COUNT_ERROR", "查询IR消息总数失败: " + e.getMessage());
        }
    }

    /**
     * [生命周期-3: 主任务节点]
     * 定义Excel的表头结构。此方法在 totalCount 之后，分发子任务之前，在主任务节点上被调用一次。
     * 这里的实现通过动态生成表头来支持多Sheet导出，每个Sheet对应一个groupIndex。
     *
     * @param user  当前操作的用户信息
     * @param query 前端传入的查询参数
     * @return 定义了所有Sheet表头的BizColumnHeaders对象
     * @throws BizException 业务异常
     */
    @Override
    public BizColumnHeaders getHeaders(BizUser user, IrMessageQuery query) throws BizException {
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 开始动态构建表头...");
        ExportMainTaskContext<?, ?, ?> context = (ExportMainTaskContext<?, ?, ?>) getContext();

        // 获取totalCount方法计算出的总数
        int totalCount = context.getExportTaskRuntimeConfig().getTotalCount();
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 获取到任务总数为: {}", totalCount);

        // 如果 totalCount 为 0，也至少创建一个 Sheet 的表头
        if (totalCount == 0) {
            totalCount = 1;
        }

        // 计算总共会产生多少个Sheet 参数 >> 配置 >> 默认
        int configSheetRowNumber = SpringContextUtil.exportProperties().getSheetRowNumber();
        Integer sheetRowNumber = Optional.ofNullable(query.getExportParams().getSheetRowNumber())
                .filter(pn -> pn > 0)
                .orElse(configSheetRowNumber);

        int totalSheets = (totalCount - 1) / sheetRowNumber + 1;
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 根据总数 {} 和单Sheet行数 {}, 计算出将生成 {} 个 Sheet", totalCount, sheetRowNumber, totalSheets);

        // 使用框架工具类从 IrMessageView.class 生成一套基础的、不带 groupIndex 的表头模板
        ColumnHeaders baseColumnHeaders = HeadersUtil.buildHeaders(null, IrMessageView.class, null);
        log.debug("[LIFECYCLE-MAIN-3] getHeaders: 从 IrMessageView.class 解析出 {} 个基础表头", baseColumnHeaders.getColumnHeaders().size());

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

        // 为了让框架知道我们计划创建 totalSheets 个Sheet，需要创建"虚拟"表头。
        // FileWriter会根据groupIndex的数量来创建Sheet。
        for (int i = 0; i < totalSheets; i++) {
            BizColumnHeaderImpl dummyHeader = new BizColumnHeaderImpl();
            dummyHeader.setFieldName("dummy_field_for_group_" + i); // 字段名不重要，但需唯一
            dummyHeader.setHeaderName(Collections.singletonList("DUMMY")); // 表头名不重要
            dummyHeader.setGroupIndex(i); // 【关键】为每个Sheet指定一个唯一的groupIndex
            bizHeaders.add(dummyHeader);
        }
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 已为 {} 个Sheet添加了虚拟表头以确保Sheet的创建", totalSheets);
        log.info("[LIFECYCLE-MAIN-3] getHeaders: 动态构建表头完成，总共定义了 {} 个表头实例 (包含虚拟表头)", bizHeaders.size());
        return bizColumnHeaders;
    }


    /**
     * [生命周期-4: 子任务节点]
     * 根据分片信息查询具体的数据。此方法会在每个子任务节点上并行执行。
     *
     * @param user           当前操作的用户信息
     * @param irMessageQuery 前端传入的查询参数
     * @param bizExportPage  框架计算好的分片信息（偏移量和大小）
     * @return 当前分片查询出的数据列表
     * @throws BizException 业务异常
     */
    @Override
    public List<IrMessageData> queryData(BizUser user, IrMessageQuery irMessageQuery, BizExportPage bizExportPage) throws BizException {
        ExportSubTaskContext<?, ?, ?> context = (ExportSubTaskContext<?, ?, ?>) getContext();
        String subTaskId = context != null ? context.getSubTask().getSubTaskId() : "UNKNOWN";
        log.info("[LIFECYCLE-SUB-1] queryData on subTask: {}: 开始查询数据分片，Offset: {}, Size: {}", subTaskId, bizExportPage.getOffset(), bizExportPage.getSize());

        // 再次获取并计算最大导出量，确保子任务不会查询超出范围的数据
//        int configTotalCount = SpringContextUtil.exportProperties().getTotalCount();
//        int maxTotalCount = Optional.ofNullable(irMessageQuery.getExportParams().getTotalCount())
//                .filter(queryTotalCount -> queryTotalCount > 0)
//                .orElse(configTotalCount);

        //将配置中央化管理
        Integer maxTotalCount = getResolvedParams(irMessageQuery).getTotalCount();

        System.out.println("effectiveExportConfig" + getResolvedParams(irMessageQuery) + "|| maxTotalCount:" + maxTotalCount);


        int pageSize = bizExportPage.getSize();
        // 如果当前分片的结束位置超过了总数限制，则调整页大小
        if (bizExportPage.getOffset() + pageSize > maxTotalCount) {
            pageSize = maxTotalCount - bizExportPage.getOffset();
            log.warn("[LIFECYCLE-SUB-1] queryData on subTask: {}: 分片大小调整，原始Size: {}, 调整后Size: {}", subTaskId, bizExportPage.getSize(), pageSize);
        }
        if (pageSize <= 0) {
            log.info("[LIFECYCLE-SUB-1] queryData on subTask: {}: 计算后的pageSize为0，无需查询，返回空列表。", subTaskId);
            return Collections.emptyList();
        }

        try {
            LambdaQueryWrapper<IrMessage> queryWrapper = irMessageQueryToirMessage(irMessageQuery);
            long pageNum = (bizExportPage.getOffset() / pageSize) + 1;
            Page<IrMessage> page = new Page<>(pageNum, pageSize);

            IPage<IrMessage> resultPage = SpringContextUtil.getIrMessageMapper().selectPage(page, queryWrapper);
            List<IrMessage> irMessages = resultPage.getRecords();

            // 转换为IrMessageData
            List<IrMessageData> dataList = new ArrayList<>();

            // 这里的操作其实可以避免,性能提升的话可以直接把查出的数据传递下去
            for (IrMessage irMessage : irMessages) {
                IrMessageData data = convertToIrMessageData(irMessage);
                dataList.add(data);
            }

            log.info("[LIFECYCLE-SUB-1] queryData on subTask: {}: 成功查询到 {} 条数据", subTaskId, dataList.size());
            return dataList;

        } catch (Exception e) {
            log.error("[LIFECYCLE-SUB-1] queryData on subTask: {}: 查询数据时发生数据库异常", subTaskId, e);
            throw new BizException("QUERY_IR_MESSAGE_DATA_ERROR", "查询IR消息数据失败: " + e.getMessage());
        }
    }

    /**
     * [生命周期-5: 子任务节点]
     * 将从数据库查询出的原始数据（DATA）转换为用于Excel展示的视图（VIEW）。
     * 此方法在每个子任务节点上并行执行，在 queryData 之后。
     *
     * @param user              当前操作的用户信息
     * @param irMessageQuery    前端传入的查询参数
     * @param irMessageDataList 当前子任务查询出的数据列表
     * @return 转换后的视图列表
     * @throws BizException 业务异常
     */
    @Override
    public List<IrMessageView> convert(BizUser user, IrMessageQuery irMessageQuery, List<IrMessageData> irMessageDataList) throws BizException {
        ExportSubTaskContext<?, ?, ?> context = (ExportSubTaskContext<?, ?, ?>) getContext();
        String subTaskId = context.getSubTask().getSubTaskId();
        int subTaskNo = context.getSubTask().getSubTaskNo(); // 把子任务编号当作页码
        log.info("[LIFECYCLE-SUB-2] convert on subTask: {}: 开始处理批次 #{} 的附件...", subTaskId, subTaskNo);

        // 1. 处理附件（支持多种模式：同步/异步/延迟/跳过）
        try {
            String mainTaskId = context.getMainTask().getMainTaskId();
            AttachmentProcessUtil.processAttachments(irMessageDataList, subTaskId, subTaskNo, irMessageQuery, mainTaskId);
        } catch (Exception e) {
            // 只记录日志，不中断导出流程
            log.error("子任务 {} 在处理附件时发生错误，但导出将继续。错误: {}", subTaskId, e.getMessage(), e);
        }

        // 2.  执行数据模型转换，生成用于Excel的View列表
        // 无论附件处理是否成功，Excel都应该正常生成
        List<IrMessageView> viewList = irMessageDataList.stream()
                .map(IrMessageUtils::createViewFromData)
                .collect(Collectors.toList());

        log.info("[LIFECYCLE-SUB-2] convert on subTask: {}: 数据转换完成。", subTaskId);
        return viewList;
    }



    /**
     * [生命周期-6: 子任务节点]
     * 将转换后的视图数据（VIEW）分组到不同的Sheet中。此方法在每个子任务节点上并行执行，在 convert 之后。
     * 这里的逻辑非常关键，它需要精确计算每条数据应该属于哪个全局Sheet。
     *
     * @param user           当前操作的用户信息
     * @param irMessageQuery 前端传入的查询参数
     * @param irMessageViews 当前子任务转换后的视图数据列表
     * @return 包含了分组后数据（可能跨越多个Sheet）的BizDataGroup
     */
    @Override
    public BizDataGroup<IrMessageView> group(BizUser user, IrMessageQuery irMessageQuery, List<IrMessageView> irMessageViews) {
        ExportSubTaskContext<?, ?, ?> context = (ExportSubTaskContext<?, ?, ?>) getContext();
        String subTaskId = context != null && context.getSubTask() != null ? context.getSubTask().getSubTaskId() : "UNKNOWN";
        log.info("[LIFECYCLE-SUB-3] group on subTask: {}: 开始对 {} 条视图数据进行分组...", subTaskId, irMessageViews.size());

        BizDataGroupImpl<IrMessageView> bizDataGroup = new BizDataGroupImpl<>();
        List<BizData<IrMessageView>> sheetDataList = new ArrayList<>();
        bizDataGroup.setData(sheetDataList);

        if (irMessageViews == null || irMessageViews.isEmpty()) {
            log.info("[LIFECYCLE-SUB-3] group on subTask: {}: 视图数据为空，返回空的DataGroup", subTaskId);
            return bizDataGroup;
        }

        // 从配置中获取每个Sheet的大小  
        int sheetSize = getResolvedParams(irMessageQuery).getSheetRowNumber();


        if (context == null) {
            log.warn("[LIFECYCLE-SUB-3] group on subTask: {}: 无法获取子任务上下文，将执行默认的单Sheet分组逻辑", subTaskId);
            return defaultGroup(irMessageViews);
        }

        int subTaskNo = context.getSubTask().getSubTaskNo();
        int pageSize = context.getExportTaskRuntimeConfig().getPageSize();
        // 计算当前子任务数据在全局数据集中的起始偏移量
        long logicalOffset = (long) (subTaskNo - 1) * pageSize;
        log.debug("[LIFECYCLE-SUB-3] group on subTask: {}: 子任务编号:{}, PageSize:{}, 计算出逻辑偏移量:{}", subTaskId, subTaskNo, pageSize, logicalOffset);

        int currentDataIndex = 0; // 迭代irMessageViews列表的指针
        while (currentDataIndex < irMessageViews.size()) {
            // 计算当前数据点在全局的逻辑位置，并确定它属于哪个Sheet
            int currentGlobalSheetIndex = (int) ((logicalOffset + currentDataIndex) / sheetSize);
            // 计算当前全局Sheet的结束位置
            long endOfCurrentSheet = (long)(currentGlobalSheetIndex + 1) * sheetSize;
            // 计算当前Sheet还剩下多少空间
            long remainingSpaceInSheet = endOfCurrentSheet - (logicalOffset + currentDataIndex);
            // 本次要写入当前Sheet的数据量，是 "Sheet剩余空间" 和 "当前子任务剩余数据量" 的较小者
            int chunkSize = (int) Math.min(remainingSpaceInSheet, irMessageViews.size() - currentDataIndex);
            if (chunkSize <= 0) {
                log.warn("[LIFECYCLE-SUB-3] group on subTask: {}: 计算出的 chunkSize 小于等于 0，跳过此次循环", subTaskId);
                currentDataIndex++; // 避免死循环
                continue;
            }

            log.debug("[LIFECYCLE-SUB-3] group on subTask: {}: 正在处理全局Sheet: {}, 逻辑偏移量:{}, 本次处理块大小:{}",
                    subTaskId, currentGlobalSheetIndex, logicalOffset + currentDataIndex, chunkSize);

            List<IrMessageView> viewsForSheet = irMessageViews.subList(currentDataIndex, currentDataIndex + chunkSize);

            // 查找或创建对应的Sheet数据容器
            BizDataGroupImpl.Data<IrMessageView> currentSheetData = findOrCreateSheetData(sheetDataList, currentGlobalSheetIndex);

            // 将数据块添加到Sheet中
            for (IrMessageView view : viewsForSheet) {
                BizDataGroupImpl.Item<IrMessageView> item = new BizDataGroupImpl.Item<>();
                item.setData(view);
                currentSheetData.getItems().add(item);
            }
            // 移动指针
            currentDataIndex += chunkSize;
        }
        log.info("[LIFECYCLE-SUB-3] group on subTask: {}: 分组完成，共生成/填充了 {} 个Sheet的数据", subTaskId, sheetDataList.size());
        return bizDataGroup;
    }


    /**
     * 在已有的Sheet数据列表中查找指定索引的Sheet，如果不存在则创建并添加。
     */
    private BizDataGroupImpl.Data<IrMessageView> findOrCreateSheetData(List<BizData<IrMessageView>> sheetDataList, int sheetIndex) {
        String sheetNoStr = String.valueOf(sheetIndex);
        // 尝试查找已存在的Sheet
        for (BizData<IrMessageView> sheetData : sheetDataList) {
            if (sheetNoStr.equals(sheetData.getMeta().get(ExcelConstants.sheetNoKey))) {
                return (BizDataGroupImpl.Data<IrMessageView>) sheetData;
            }
        }
        // 如果不存在，则创建一个新的
        BizDataGroupImpl.Data<IrMessageView> newSheetData = new BizDataGroupImpl.Data<>();
        String sheetName = "数据Sheet-" + (sheetIndex + 1);
        Map<String, String> meta = new HashMap<>();
        meta.put(ExcelConstants.sheetNameKey, sheetName);
        meta.put(ExcelConstants.sheetNoKey, sheetNoStr);
        newSheetData.setMeta(meta);
        newSheetData.setItems(new ArrayList<>());
        sheetDataList.add(newSheetData);
        return newSheetData;
    }



    /**
     * 一个备用的、简单的group实现，用于处理无法获取子任务上下文的边缘情况。
     */
    private BizDataGroup<IrMessageView> defaultGroup(List<IrMessageView> views) {
        BizDataGroupImpl<IrMessageView> group = new BizDataGroupImpl<>();
        BizDataGroupImpl.Data<IrMessageView> data = new BizDataGroupImpl.Data<>();
        List<BizData<IrMessageView>> dataList = new ArrayList<>();
        dataList.add(data);
        group.setData(dataList);

        Map<String, String> meta = new HashMap<>();
        meta.put(ExcelConstants.sheetNameKey, "Sheet1");
        meta.put(ExcelConstants.sheetNoKey, "0");
        data.setMeta(meta);

        List<BizDataItem<IrMessageView>> items = new ArrayList<>();
        data.setItems(items);
        for (IrMessageView view : views) {
            BizDataGroupImpl.Item<IrMessageView> item = new BizDataGroupImpl.Item<>();
            item.setData(view);
            items.add(item);
        }
        return group;
    }



}