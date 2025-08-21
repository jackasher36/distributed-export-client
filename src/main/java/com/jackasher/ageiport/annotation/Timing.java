package com.jackasher.ageiport.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 计时注解，用于自动记录方法执行时间
 * @author Jackasher
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timing {
    
    /**
     * 自定义描述信息
     */
    String value() default "";
    
    /**
     * 是否在info级别记录日志，默认为true
     * false时在debug级别记录
     */
    boolean info() default true;
    
    /**
     * 时间单位：ms(毫秒)、s(秒)
     */
    String unit() default "ms";
}
