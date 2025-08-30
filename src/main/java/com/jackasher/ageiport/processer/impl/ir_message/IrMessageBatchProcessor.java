package com.jackasher.ageiport.processer.impl.ir_message;

import java.util.List;

import org.springframework.stereotype.Component;

import com.jackasher.ageiport.dispatcher.GenericProcessingDispatcher;
import com.jackasher.ageiport.model.ir_message.IrMessageData;
import com.jackasher.ageiport.model.ir_message.IrMessageQuery;
import com.jackasher.ageiport.processer.GenericBatchProcessor;
import com.jackasher.ageiport.service.data_processing_service.GenericDataProcessingService;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;

/**
 * IrMessage批处理器实现
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Component
public class IrMessageBatchProcessor implements GenericBatchProcessor<IrMessageData, IrMessageQuery> {

    @Override
    public void processBatchData(List<IrMessageData> dataList, String subTaskId, int pageNum, IrMessageQuery query, String mainTaskId) {
        // 使用现有的批处理调度器
        @SuppressWarnings("unchecked")
        GenericDataProcessingService<IrMessageData, IrMessageQuery> service = 
            (GenericDataProcessingService<IrMessageData, IrMessageQuery>) SpringContextUtil.getBean("attachmentProcessingServiceImpl", GenericDataProcessingService.class);
        GenericProcessingDispatcher<IrMessageData, IrMessageQuery> dispatcher = new GenericProcessingDispatcher<>(service);
        dispatcher.processBatchData(dataList, subTaskId, pageNum, query, mainTaskId);
    }
}