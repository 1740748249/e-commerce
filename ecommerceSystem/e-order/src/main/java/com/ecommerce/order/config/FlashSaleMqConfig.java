package com.ecommerce.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ecommerce.common.constants.MqConstants.Exchange.ORDER_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_FLASH_CREATE_KEY;
import static com.ecommerce.order.constants.MqConstants.FLASH_ORDER_CREATE_QUEUE;

@Configuration
public class FlashSaleMqConfig {

    @Bean
    public Queue flashOrderCreateQueue() {
        return QueueBuilder.durable(FLASH_ORDER_CREATE_QUEUE).build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding flashOrderCreateBinding(Queue flashOrderCreateQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(flashOrderCreateQueue).to(orderExchange).with(ORDER_FLASH_CREATE_KEY);
    }
}
