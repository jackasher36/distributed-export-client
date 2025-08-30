package com.jackasher.ageiport.processer.impl.ir_message;

import com.alibaba.ageiport.processor.core.annotation.ExportSpecification;
import com.alibaba.ageiport.processor.core.constants.ExecuteType;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.model.ir_message.IrMessageView;
import com.jackasher.ageiport.processer.GenericBatchProcessor;
import com.jackasher.ageiport.processer.GenericDataAccessor;
import com.jackasher.ageiport.processer.GenericDataConverter;
import com.jackasher.ageiport.processer.GenericExportAdapter;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;


@ExportSpecification(
        code = "IrMessageExportProcessor",
        name = "IrMessageExportProcessor",
        executeType = ExecuteType.CLUSTER
)
public class IrMessageExportProcessor extends GenericExportAdapter<IrMessageQuery, IrMessageData, IrMessageView> {

    @Override
    protected GenericDataAccessor<IrMessageQuery, IrMessageData> getDataAccessor() {
        return SpringContextUtil.getIrMessageDataAccessor();
    }

    @Override
    protected GenericDataConverter<IrMessageData, IrMessageView> getDataConverter() {
        return SpringContextUtil.getIrMessageDataConverter();
    }

    @Override
    protected GenericBatchProcessor<IrMessageData, IrMessageQuery> getBatchProcessor() {
        return SpringContextUtil.getIrMessageBatchProcessor();
    }

    @Override
    protected Class<IrMessageView> getViewClass() {
        return IrMessageView.class;
    }

    @Override
    protected String getExportCode() {
        return "IrMessage";
    }
}