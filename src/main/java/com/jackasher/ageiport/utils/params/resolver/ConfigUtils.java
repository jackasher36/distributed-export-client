// src/main/java/com/jackasher/ageiport/utils/ConfigUtils.java
package com.jackasher.ageiport.utils.params.resolver;

import java.util.Optional;

/**
 * 通用的配置参数选择器工具类。
 * 封装了“API参数优先于默认配置”的核心逻辑。
 */
public class ConfigUtils {

    /**
     * 通用的参数选择器。如果API提供的值为null，则使用默认值。
     * @param apiValue 来自 API 的值 (必须是包装类型, 如 Integer, Boolean)
     * @param defaultValue 来自配置文件的默认值
     * @param <T> 值的类型
     * @return 最终生效的值
     */
    public static <T> T choose(T apiValue, T defaultValue) {
        return Optional.ofNullable(apiValue).orElse(defaultValue);
    }

    /**
     * 带正数校验的参数选择器 (针对数字类型)。
     * 如果API提供的值为null或小于等于0，则使用默认值。
     * @param apiValue 来自 API 的数字值 (Integer, Long)
     * @param defaultValue 来自配置文件的默认值
     * @param <N> 必须是 Number 的子类
     * @return 最终生效的值
     */
    public static <N extends Number> N choosePositive(N apiValue, N defaultValue) {
        return Optional.ofNullable(apiValue)
                .filter(n -> n.doubleValue() > 0)
                .orElse(defaultValue);
    }

    /**
     * 带非空字符串校验的参数选择器。
     * 如果API提供的值为null或空白字符串，则使用默认值。
     * @param apiValue 来自 API 的字符串值
     * @param defaultValue 来自配置文件的默认值
     * @return 最终生效的值
     */
    public static String chooseNonBlank(String apiValue, String defaultValue) {
        return Optional.ofNullable(apiValue)
                .filter(s -> !s.trim().isEmpty())
                .orElse(defaultValue);
    }
}