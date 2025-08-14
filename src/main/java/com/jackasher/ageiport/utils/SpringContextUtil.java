package com.jackasher.ageiport.utils;

import com.jackasher.ageiport.config.export.ExportProperties;
import com.jackasher.ageiport.mapper.IrMessageMapper;
import io.minio.MinioClient;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Spring上下文工具类，用于在非Spring管理的类中获取Spring Bean
 * @author Jackasher
 * @version 1.0
 * @since 1.0
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    /**
     * -- GETTER --
     *  获取ApplicationContext
     */
    @Getter
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
    }

    /**
     * 通过class获取Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过name获取Bean
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过name和class获取指定的Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

    // --- 线程安全地获取特定 Bean 的方法 ---

    /**
     * 延迟获取IrMessageMapper实例
     * 解决AgeiPort框架创建processor时依赖注入失效的问题
     */
    public static IrMessageMapper getIrMessageMapper() {
        return IrMessageMapperHolder.INSTANCE;
    }

    private static class IrMessageMapperHolder {
        private static final IrMessageMapper INSTANCE = SpringContextUtil.getBean(IrMessageMapper.class);
    }



    /**
     * 获取导出配置
     */
    public static ExportProperties exportProperties() {
        return ExportPropertiesHolder.INSTANCE;
    }

    private static class ExportPropertiesHolder {
        private static final ExportProperties INSTANCE = SpringContextUtil.getBean(ExportProperties.class);
    }

    /**
     * 获取 MinioClient
     */
    public static MinioClient minioClient() {
        return MinioClientHolder.INSTANCE;
    }

    private static class MinioClientHolder {
        private static final MinioClient INSTANCE = SpringContextUtil.getBean(MinioClient.class);
    }
}
