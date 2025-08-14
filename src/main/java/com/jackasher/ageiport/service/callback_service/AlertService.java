package com.jackasher.ageiport.service.callback_service;

import org.springframework.stereotype.Service;
import com.alibaba.ageiport.common.logger.Logger;
import com.alibaba.ageiport.common.logger.LoggerFactory;

@Service
public class AlertService {
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);

    public void sendAlert(String title, String content) {
        // TODO: 实现告警通知，例如发送邮件、钉钉消息等
        logger.error("!!! 【ALERT】 !!!\nTitle: {}\nContent: {}\n", title, content);
    }
}