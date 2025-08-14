// src/main/java/com/jackasher/ageiport/utils/ConfigMerger.java
package com.jackasher.ageiport.utils.params.reflect;

import org.springframework.util.ReflectionUtils;
import java.lang.reflect.Field;

/**
 * 通用的、基于反射的配置合并工具。
 */
public class ConfigMerger {

    /**
     * 将默认配置对象中的值，填充到API参数对象中的null字段。
     * @param apiParams 来源于API请求的参数对象，可能包含null值。它将被直接修改。
     * @param defaultProps 包含所有默认值的配置对象。
     * @param <A> API参数对象的类型
     * @param <D> 默认配置对象的类型
     */
    public static <A, D> void merge(A apiParams, D defaultProps) {
        if (apiParams == null || defaultProps == null) {
            return;
        }

        // 遍历API参数对象的所有字段
        for (Field apiField : apiParams.getClass().getDeclaredFields()) {
            try {
                ReflectionUtils.makeAccessible(apiField);

                // 获取API字段的当前值
                Object apiValue = apiField.get(apiParams);

                //  如果API字段的值为null，才进行填充
                if (apiValue == null) {
                    // 在默认配置对象中查找同名字段
                    Field defaultField = ReflectionUtils.findField(defaultProps.getClass(), apiField.getName());
                    
                    if (defaultField != null) {
                        ReflectionUtils.makeAccessible(defaultField);
                        Object defaultValue = defaultField.get(defaultProps);
                        
                        // 将默认值设置到API对象的字段中
                        apiField.set(apiParams, defaultValue);
                    }
                }
            } catch (IllegalAccessException e) {
                // 在实际项目中，这里应该有更健壮的日志和异常处理
                throw new RuntimeException("配置合并失败，字段: " + apiField.getName(), e);
            }
        }
    }
}