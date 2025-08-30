package com.jackasher.ageiport.processer.impl.user;

import javax.annotation.Resource;

import com.jackasher.ageiport.model.user.UserQuery;
import com.jackasher.ageiport.processer.GenericBatchProcessor;
import com.jackasher.ageiport.processer.GenericDataAccessor;
import com.jackasher.ageiport.processer.GenericDataConverter;
import com.jackasher.ageiport.processer.GenericExportAdapter;
import org.springframework.stereotype.Component;

import com.alibaba.ageiport.processor.core.annotation.ExportSpecification;
import com.alibaba.ageiport.processor.core.constants.ExecuteType;
import com.jackasher.ageiport.model.user.UserData;
import com.jackasher.ageiport.model.user.UserView;

/**
 * 用户导出处理器示例
 * 展示如何快速为新的数据类型实现导出功能
 * 只需要50行代码就完成了完整的导出流程！
 *
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 **/
@Component
@ExportSpecification(
        code = "UserExportProcessor",
        name = "用户导出处理器",
        executeType = ExecuteType.CLUSTER
)
public class UserExportProcessor extends GenericExportAdapter<UserQuery, UserData, UserView> {

    @Resource
    private UserDataAccessor dataAccessor;

    @Resource
    private UserDataConverter dataConverter;

    @Resource
    private UserBatchProcessor batchProcessor;

    @Override
    protected GenericDataAccessor<UserQuery, UserData> getDataAccessor() {
        return dataAccessor;
    }

    @Override
    protected GenericDataConverter<UserData, UserView> getDataConverter() {
        return dataConverter;
    }

    @Override
    protected GenericBatchProcessor<UserData, UserQuery> getBatchProcessor() {
        return batchProcessor;
    }

    @Override
    protected Class<UserView> getViewClass() {
        return UserView.class;
    }

    @Override
    protected String getExportCode() {
        return "User";
    }
}