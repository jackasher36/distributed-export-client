package com.jackasher.ageiport.service.callback_service;

import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BusinessTaskServiceImpl implements BusinessTaskService {
    private static final Logger logger = LoggerFactory.getLogger(BusinessTaskServiceImpl.class);

    @Override
    public void createTaskRecord(String bizKey, String mainTaskId, String userId, String initialStatus) {
        // TODO: 在业务数据库中插入或更新一条记录
        // 例如: INSERT INTO export_tasks (biz_key, ageiport_task_id, user_id, status, create_time) VALUES (?, ?, ?, ?, NOW());
        logger.info("【业务数据库】: 任务记录创建. BizKey: {}, TaskId: {}, UserId: {}, Status: {}", bizKey, mainTaskId, userId, initialStatus);
    }

    @Override
    public void updateTaskSuccess(String bizKey, String message, String downloadUrl) {
        // TODO: 更新业务数据库中的任务记录
        // 例如: UPDATE export_tasks SET status = 'COMPLETED', message = ?, download_url = ?, finish_time = NOW() WHERE biz_key = ?;
        logger.info("【业务数据库】: 任务成功. BizKey: {}, Message: {}, DownloadUrl: {}", bizKey, message, downloadUrl);
    }

    @Override
    public void updateTaskFailure(String bizKey, String errorMessage) {
        // TODO: 更新业务数据库中的任务记录
        // 例如: UPDATE export_tasks SET status = 'FAILED', message = ?, finish_time = NOW() WHERE biz_key = ?;
        logger.error("【业务数据库】: 任务失败. BizKey: {}, ErrorMessage: {}", bizKey, errorMessage);
    }
}