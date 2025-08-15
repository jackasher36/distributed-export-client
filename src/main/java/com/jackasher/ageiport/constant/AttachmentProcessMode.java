package com.jackasher.ageiport.constant;

/**
 * 附件处理模式
 * 
 * @author jackasher
 */
public enum AttachmentProcessMode {
    
    /**
     * 同步处理 - 阻塞当前线程
     */
    SYNC,
    
    /**
     * 异步处理 - 立即提交到线程池
     */
    ASYNC,
    
    /**
     * 延迟处理 - 任务完成后再处理
     */
    DEFERRED,
    
    /**
     * 不处理
     */
    NONE
}
