package com.ecommerce.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ecommerce.common.constants.MqConstants.Exchange.DELAY_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Exchange.PRODUCT_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_DELAY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.STOCK_SYNC_KEY;
import static com.ecommerce.order.constants.MqConstants.ORDER_DELAY_QUEUE;
import static com.ecommerce.order.constants.MqConstants.STOCK_SYNC_QUEUE;

@Configuration
public class OrderMqConfig {

    @Bean
    public Queue stockSyncQueue() {
        return QueueBuilder.durable(STOCK_SYNC_QUEUE).build();
    }

    @Bean
    public Binding stockSyncBinding(Queue stockSyncQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(stockSyncQueue).to(productExchange).with(STOCK_SYNC_KEY);
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE).build();
    }

    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, Exchange delayExchange) {
        return new Binding(orderDelayQueue.getName(), Binding.DestinationType.QUEUE,
                delayExchange.getName(), ORDER_DELAY_KEY, null);
    }
}
