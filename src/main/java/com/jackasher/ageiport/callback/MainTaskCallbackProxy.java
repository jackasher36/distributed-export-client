package com.jackasher.ageiport.callback;

import com.alibaba.ageiport.processor.core.model.core.impl.MainTask;
import com.jackasher.ageiport.constant.MainTaskCallbackConstant;
import com.jackasher.ageiport.utils.SpringContextUtil;
import org.springframework.context.ApplicationContext;

/**
 * AGEIPort SPI 代理回调实现。
 * 这个类由 AGEIPort 的 ExtensionLoader 通过反射直接实例化，不进行依赖注入。
 * 它的唯一作用是作为跳板，从 Spring 容器中获取真正实现了业务逻辑的 MainTaskCallback Bean，
 * 并将所有方法调用委托给它。
 * 这种方式可以完美解决 AGEIPort 初始化与 Spring Bean 生命周期的冲突问题。
 */
public class MainTaskCallbackProxy implements com.alibaba.ageiport.processor.core.spi.task.callback.MainTaskCallback {

    private volatile com.alibaba.ageiport.processor.core.spi.task.callback.MainTaskCallback delegate;

    /**
     * 延迟加载真正的 Spring Bean 代理。
     * 使用双重检查锁定确保线程安全和性能。
     */
    private com.alibaba.ageiport.processor.core.spi.task.callback.MainTaskCallback getDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    ApplicationContext context = SpringContextUtil.getApplicationContext();
                    if (context == null) {
                        // 在 Spring 上下文准备好之前，这是一个预期的临时状态，可以抛出异常或等待
                        throw new IllegalStateException("Spring ApplicationContext not ready yet. Cannot get MainTaskCallback bean.");
                    }
                    // 从Spring容器中获取我们真正的、功能齐全的Callback Bean
                    this.delegate = context.getBean(MainTaskCallbackConstant.MAIN_TASK_CALLBACK_BEAN_NAME,
                            com.alibaba.ageiport.processor.core.spi.task.callback.MainTaskCallback.class);
                }
            }
        }
        return delegate;
    }

    @Override
    public void afterCreated(MainTask mainTask) {
        getDelegate().afterCreated(mainTask);
    }

    @Override
    public void beforeFinished(MainTask mainTask) {
        getDelegate().beforeFinished(mainTask);
    }

    @Override
    public void afterFinished(MainTask mainTask) {
        getDelegate().afterFinished(mainTask);
    }

    @Override
    public void beforeError(MainTask mainTask) {
        getDelegate().beforeError(mainTask);
    }

    @Override
    public void afterError(MainTask mainTask) {
        getDelegate().afterError(mainTask);
    }
}