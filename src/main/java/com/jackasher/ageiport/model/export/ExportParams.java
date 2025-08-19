package com.jackasher.ageiport.model.export;

import com.jackasher.ageiport.constant.AttachmentProcessMode;
import lombok.Data;

import java.io.Serializable;


/**
 * @author Jackasher
 * @className ExportParams
 * 用于导出任务参数配置,会覆盖原有的配置文件配置
 **/
@Data
public class ExportParams implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 导出数据总条数
     */
    private Integer totalCount;

    /**
     * 单个Excel文件数据条数
     */
    private Integer excelRowNumber;

    /**
     * 单个Sheet数据条数
     */
    private Integer sheetRowNumber;

    /**
     * 分页查询每页数据条数
     */
    private Integer pageRowNumber;

    /**
     * 任务结束生成压缩包后是否删除生成的Excel等临时文件
     */
    private Boolean deleteTempFile;

    /**
     * 文件暂存目录,兼容相对路径/绝对路径
     */
    private String fileTempDirectory;

    /**
     * 导出任务超时时间/单位毫秒
     */
    private Long taskTimeout;

    /**
     * 默认Minio桶名称
     */
    private String defaultBucketName;

    /**
     * 线程池配置
     */
    private ThreadPool threadPool;

    /**
     * 导出结束后是否删除文件,此项配置由业务服务自定义
     */
    private Boolean deleteFileAfterExport;

    //============== 与项目集成的参数 ================

    /**
     * 是否处理附件
     */
    private Boolean processAttachments;

    /**
     * 附件处理模式：SYNC/ASYNC/DEFERRED/NONE
     */
    private AttachmentProcessMode attachmentProcessMode;

    /**
     * excel文件保存目录
     */
    private String excelFileDirectory = System.getProperty("user.home");

    /**
     * excel文件名
     */
    private String  excelFileName;



    // 构造函数中初始化嵌套对象，以避免NullPointerException
    public ExportParams() {
        this.threadPool = new ThreadPool();
    }


    /**
     * 内部静态类，用于映射 thread-pool 配置
     */
    @Data
    public static class ThreadPool implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 核心线程数量
         */
        private Integer threadSize;

        /**
         * 最大线程数量
         */
        private Integer maximumPoolSize;

        // 无参构造函数，虽然Lombok的@Data会生成，但明确写出也无妨
        public ThreadPool() {}
    }
}
