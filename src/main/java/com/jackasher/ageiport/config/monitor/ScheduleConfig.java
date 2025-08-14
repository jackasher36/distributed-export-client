package com.jackasher.ageiport.config.monitor;

import java.util.concurrent.Executors;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 定时任务配置
 * 为内存监控等定时任务提供独立的线程池
 * 
 * @author jackasher
 */
@Configuration
@EnableScheduling
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 为定时任务创建独立的线程池，避免影响主业务线程
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "memory-monitor-scheduler");
            t.setDaemon(true); // 设置为守护线程
            return t;
        }));
    }
}
