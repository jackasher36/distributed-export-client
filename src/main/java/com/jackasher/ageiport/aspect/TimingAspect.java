package com.jackasher.ageiport.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.jackasher.ageiport.annotation.Timing;

/**
 * 计时切面，用于自动记录方法执行时间
 * @author Jackasher
 */
@Aspect
@Component
public class TimingAspect {
    
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);
    
    @Around("@annotation(timing)")
    public Object timeMethod(ProceedingJoinPoint joinPoint, Timing timing) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行原方法
            Object result = joinPoint.proceed();
            
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            // 记录日志
            logExecutionTime(joinPoint, timing, executionTime, null);
            
            return result;
        } catch (Throwable throwable) {
            // 即使出现异常也要记录执行时间
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            logExecutionTime(joinPoint, timing, executionTime, throwable);
            
            throw throwable;
        }
    }
    
    private void logExecutionTime(ProceedingJoinPoint joinPoint, Timing timing, long executionTimeMs, Throwable throwable) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        
        // 构建描述信息
        String description = timing.value().isEmpty() ? 
            String.format("%s.%s", className, methodName) : 
            timing.value();
        
        // 根据单位转换时间
        String timeStr = formatTime(executionTimeMs, timing.unit());
        
        // 构建日志消息
        String logMessage = throwable == null ? 
            String.format("⏱️[%s]-------> [%s] 执行时间: %s",methodName, description, timeStr) :
            String.format("⏱️[%s]-------> [%s] 执行时间: %s (异常: %s)",methodName, description, timeStr, throwable.getClass().getSimpleName());
        
        // 根据配置记录日志
        if (timing.info()) {
            log.info(logMessage);
        } else {
            log.debug(logMessage);
        }
    }
    
    private String formatTime(long timeMs, String unit) {
        switch (unit.toLowerCase()) {
            case "s":
                return String.format("%.3fs", timeMs / 1000.0);
            case "ms":
            default:
                return timeMs + "ms";
        }
    }
}
