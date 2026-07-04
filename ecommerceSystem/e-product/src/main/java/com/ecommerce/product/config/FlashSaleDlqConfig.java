package com.ecommerce.product.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ecommerce.common.constants.MqConstants.Exchange.ERROR_EXCHANGE;

@Configuration
public class FlashSaleDlqConfig {

    public static final String FLASH_ORDER_DLQ = "flash.order.create.dlq";
    private static final String ROUTING_KEY = "error.order-service";

    @Bean
    public Queue flashOrderDlq() {
        return new Queue(FLASH_ORDER_DLQ, true);
    }

    @Bean
    public Binding flashOrderDlqBinding(Queue flashOrderDlq, DirectExchange errorMessageExchange) {
        return BindingBuilder.bind(flashOrderDlq).to(errorMessageExchange).with(ROUTING_KEY);
    }
}
