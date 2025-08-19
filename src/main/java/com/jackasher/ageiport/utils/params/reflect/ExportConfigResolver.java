// src/main/java/com/jackasher/ageiport/service/ExportConfigResolver.java
package com.jackasher.ageiport.utils.params.reflect;

import com.jackasher.ageiport.config.export.ExportProperties;
import com.jackasher.ageiport.model.export.ExportParams;
import com.jackasher.ageiport.utils.ioc.SpringContextUtil;
import org.springframework.stereotype.Service;

@Service
public class ExportConfigResolver {

    /**
     * 使用通用的反射工具，将默认配置填充到API参数对象中。
     *
     * @param apiParams 来自前端API请求的参数对象。
     * @return 返回被填充完善后的同一个 apiParams 对象。
     */
    public ExportParams resolve(ExportParams apiParams) {
        // 1. 获取默认配置
        ExportProperties defaultProps = SpringContextUtil.exportProperties();

        // 2. 一行代码，完成所有字段的合并！
        ConfigMerger.merge(apiParams, defaultProps);

        // 3. 返回被修改后的对象
        return apiParams;
    }
}