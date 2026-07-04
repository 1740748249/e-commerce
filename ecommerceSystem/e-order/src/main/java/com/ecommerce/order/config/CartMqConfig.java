package com.ecommerce.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ecommerce.common.constants.MqConstants.Exchange.DELAY_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.CART_SYNC_KEY;
import static com.ecommerce.order.constants.MqConstants.CART_SYNC_QUEUE;

@Configuration
public class CartMqConfig {

    @Bean
    public Queue cartSyncQueue() {
        return QueueBuilder.durable(CART_SYNC_QUEUE).build();
    }

    @Bean
    public Binding cartSyncBinding(Queue cartSyncQueue, Exchange delayExchange) {
        return new Binding(cartSyncQueue.getName(), Binding.DestinationType.QUEUE,
                delayExchange.getName(), CART_SYNC_KEY, null);
    }
}
