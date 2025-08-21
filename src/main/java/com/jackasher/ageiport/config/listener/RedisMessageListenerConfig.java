// src/main/java/com/jackasher/ageiport/config/redis/RedisMessageListenerConfig.java

package com.jackasher.ageiport.config.listener;


import com.jackasher.ageiport.listener.RedisDeferredTaskSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisMessageListenerConfig {
    
    public static final String DEFERRED_TASK_TRIGGER_CHANNEL = "ageiport:deferred_task_trigger";

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic(DEFERRED_TASK_TRIGGER_CHANNEL));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(RedisDeferredTaskSubscriber subscriber) {
        // 将消息委托给 subscriber 的 handleMessage 方法处理
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }
}