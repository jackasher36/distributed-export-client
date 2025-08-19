// src/main/java/com/jackasher/ageiport/config/mq/RabbitMqConfig.java
package com.jackasher.ageiport.mq.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue attachmentQueue() {
        return new Queue("attachment_process_queue", true);
    }

    @Bean
    public TopicExchange attachmentExchange() {
        return new TopicExchange(MqProducerService.ATTACHMENT_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue attachmentQueue, TopicExchange attachmentExchange) {
        return BindingBuilder.bind(attachmentQueue).to(attachmentExchange).with(MqProducerService.ATTACHMENT_ROUTING_KEY);
    }

    /**
     * 配置JSON消息转换器，替代默认的Java序列化
     * 这样可以避免Java序列化的问题，并且消息更加可读
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate使用JSON消息转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}