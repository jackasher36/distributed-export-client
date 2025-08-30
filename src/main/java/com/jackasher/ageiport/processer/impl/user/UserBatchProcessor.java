package com.jackasher.ageiport.processer.impl.user;

import java.util.List;

import com.jackasher.ageiport.dispatcher.GenericProcessingDispatcher;
import com.jackasher.ageiport.model.user.UserQuery;
import com.jackasher.ageiport.service.data_processing_service.GenericDataProcessingService;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jackasher.ageiport.model.user.UserData;
import com.jackasher.ageiport.processer.GenericBatchProcessor;

/**
 * 用户批处理器实现示例
 * 展示如何为其他数据类型实现批处理逻辑
 * 
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Component
public class UserBatchProcessor implements GenericBatchProcessor<UserData, UserQuery> {

    private static final Logger log = LoggerFactory.getLogger(UserBatchProcessor.class);

    @Override
    public void processBatchData(List<UserData> dataList, String subTaskId, int pageNum, UserQuery query, String mainTaskId) {
        GenericProcessingDispatcher<UserData, UserQuery> dispatcher =
                new GenericProcessingDispatcher<UserData, UserQuery>(SpringContextUtil.getBean("test", GenericDataProcessingService.class));
        dispatcher.processBatchData(dataList, subTaskId, pageNum, query, mainTaskId);
    }
}