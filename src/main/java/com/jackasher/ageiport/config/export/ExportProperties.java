package com.jackasher.ageiport.config.export;

/**
 * @author Jackasher
 * @version 1.0
 * @className ExportProperties
 * @since 1.0
 **/

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 映射 application.yml 中 ageiport.export 相关配置的类
 */
@Component
@Data
@ConfigurationProperties(prefix = "ageiport.export")
@RefreshScope
public class ExportProperties {
    /**
     * 导出任务总数据量
     */
    private int totalCount = 10000;

    /**
     * 单个Excel文件数据条数,默认100W条
     */
    private int excelRowNumber = 1_000_000;

    /**
     * 单个Sheet数据条数,默认1W条
     */
    private int sheetRowNumber = 10_000;

    /**
     * 分页查询每页数据条数,默认5000条
     */
    private int pageRowNumber = 5_000;

    /**
     * 任务结束生成压缩包后是否删除生成的Excel等临时文件
     */
    private boolean deleteTempFile = true;

    /**
     * 文件暂存目录,兼容相对路径/绝对路径
     */
    private String fileTempDirectory = "excel";

    /**
     * 导出任务超时时间/单位毫秒,默认24小时
     */
    private long taskTimeout = 86400000L;

    /**
     * 默认Minio桶名称
     */
    private String defaultBucketName = "ageiport-tasks";

    /**
     * 线程池配置
     */
    private ThreadPool threadPool = new ThreadPool();

    /**
     * 导出结束后是否删除文件,此项配置由业务服务自定义
     */
    private boolean deleteFileAfterExport = false;


    //============== 与项目集成的参数 ================
    /**
     * excel文件保存目录
     */
    private String excelFileDirectory = System.getProperty("user.home");

    /**
     * 是否处理附件
     */
    private boolean processAttachments = false;

    /**
     * 导出excel文件名
     */
    private String excelFileName = "default";

    /**
     * 附件处理模式：SYNC/ASYNC/DEFERRED/NONE
     */
    private String attachmentProcessMode = "ASYNC";


    /**
     * 内部静态类，用于映射 thread-pool 配置
     */
    @Data
    public static class ThreadPool {

        /**
         * 核心线程数量,默认0
         */
        private int threadSize = 0;

        /**
         * 最大线程数量,默认8
         */
        private int maximumPoolSize = 8;

    }
}