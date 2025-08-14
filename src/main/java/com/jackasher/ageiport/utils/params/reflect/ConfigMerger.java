// src/main/java/com/jackasher/ageiport/utils/ConfigMerger.java
package com.jackasher.ageiport.utils.params.reflect;

import java.lang.reflect.Field;

import org.springframework.util.ReflectionUtils;

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

        System.out.println("【ConfigMerger开始】apiParams类型: " + apiParams.getClass().getName());
        System.out.println("【ConfigMerger开始】defaultProps类型: " + defaultProps.getClass().getName());

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
                        // 尝试通过 getter 方法获取值（处理 Spring CGLIB 代理）
                        Object defaultValue = getValueByGetter(defaultProps, apiField.getName());
                        
                        // 如果 getter 方法失败，回退到直接字段访问
                        if (defaultValue == null) {
                            ReflectionUtils.makeAccessible(defaultField);
                            defaultValue = defaultField.get(defaultProps);
                        }

//                        System.out.println("【ConfigMerger处理】字段: " + apiField.getName() +
//                                         ", 默认值: " + defaultValue +
//                                         " (类型: " + (defaultValue != null ? defaultValue.getClass().getSimpleName() : "null") + ")");

                        // 将默认值设置到API对象的字段中
                        apiField.set(apiParams, defaultValue);
                        
                        // 验证设置是否成功
                        Object newValue = apiField.get(apiParams);
//                        System.out.println("【ConfigMerger验证】字段: " + apiField.getName() +
//                                         " 设置后的值: " + newValue);
                    } else {
                        System.out.println("【ConfigMerger跳过】字段: " + apiField.getName() + 
                                         " - 在默认配置中未找到对应字段");
                    }
                } else {
//                    System.out.println("【ConfigMerger跳过】字段: " + apiField.getName() +
//                                     " - API值非null: " + apiValue);
                }
            } catch (IllegalAccessException e) {
                // 在实际项目中，这里应该有更健壮的日志和异常处理
                throw new RuntimeException("配置合并失败，字段: " + apiField.getName(), e);
            }
        }
    }

    /**
     * 通过 getter 方法获取值，处理 Spring CGLIB 代理问题
     */
    private static Object getValueByGetter(Object obj, String fieldName) {
        try {
            // 构造 getter 方法名
            String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            
            // 查找 getter 方法
            java.lang.reflect.Method getter = ReflectionUtils.findMethod(obj.getClass(), getterName);
            
            // 如果没有找到 get 方法，尝试 is 方法（用于 boolean 类型）
            if (getter == null) {
                String isGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                getter = ReflectionUtils.findMethod(obj.getClass(), isGetterName);
            }
            
            if (getter != null) {
                ReflectionUtils.makeAccessible(getter);
                Object result = getter.invoke(obj);
                System.out.println("【ConfigMerger调试】通过getter " + getter.getName() + " 获取字段 " + fieldName + " 值: " + result);
                return result;
            }
        } catch (Exception e) {
            System.out.println("【ConfigMerger警告】通过getter获取字段 " + fieldName + " 失败: " + e.getMessage());
        }
        return null;
    }
}