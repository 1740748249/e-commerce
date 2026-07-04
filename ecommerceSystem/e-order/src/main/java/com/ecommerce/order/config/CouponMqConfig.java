package com.ecommerce.order.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ecommerce.common.constants.MqConstants.Exchange.COUPON_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.COUPON_CLAIM_KEY;
import static com.ecommerce.order.constants.MqConstants.COUPON_CLAIM_QUEUE;

@Configuration
public class CouponMqConfig {

    @Bean
    public Queue couponClaimQueue() {
        return QueueBuilder.durable(COUPON_CLAIM_QUEUE).build();
    }

    @Bean
    public TopicExchange couponExchange() {
        return new TopicExchange(COUPON_EXCHANGE);
    }

    @Bean
    public Binding couponClaimBinding(Queue couponClaimQueue, TopicExchange couponExchange) {
        return BindingBuilder.bind(couponClaimQueue).to(couponExchange).with(COUPON_CLAIM_KEY);
    }
}
