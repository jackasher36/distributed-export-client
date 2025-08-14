package com.jackasher.ageiport.service.callback_service;

public interface BusinessTaskService {
    void createTaskRecord(String bizKey, String mainTaskId, String userId, String initialStatus);
    void updateTaskSuccess(String bizKey, String message, String downloadUrl);
    void updateTaskFailure(String bizKey, String errorMessage);
}