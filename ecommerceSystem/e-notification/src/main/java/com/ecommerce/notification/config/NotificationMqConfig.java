package com.ecommerce.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ecommerce.common.constants.MqConstants.Exchange.ORDER_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_NOTIFY_KEY;
import static com.ecommerce.notification.constants.MqConstants.ORDER_NOTIFY_QUEUE;

@Configuration
public class NotificationMqConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderNotifyQueue() {
        return new Queue(ORDER_NOTIFY_QUEUE, true);
    }

    @Bean
    public Binding orderNotifyBinding(Queue orderNotifyQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderNotifyQueue).to(orderExchange).with(ORDER_NOTIFY_KEY);
    }
}
