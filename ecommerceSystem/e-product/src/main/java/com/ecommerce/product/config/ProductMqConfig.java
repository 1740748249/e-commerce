package com.ecommerce.product.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ecommerce.common.constants.MqConstants.Exchange.DELAY_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Exchange.ERROR_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Exchange.PRODUCT_EXCHANGE;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_TIMEOUT_DELAY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_ORDER_PAID_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_SALE_REFUND_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_STOCK_RESTORE_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.FLASH_STOCK_SYNC_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_CANCELLED_NOTIFY_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.ORDER_CREATE_FAILED_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.STOCK_RESTORE_KEY;
import static com.ecommerce.common.constants.MqConstants.Key.STOCK_SYNC_KEY;

@Configuration
public class ProductMqConfig {

    @Bean
    public Queue stockSyncQueue() {
        return QueueBuilder.durable("stock.sync.queue").build();
    }

    @Bean
    public Binding stockSyncBinding(Queue stockSyncQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(stockSyncQueue).to(productExchange).with(STOCK_SYNC_KEY);
    }

    @Bean
    public Queue stockRestoreQueue() {
        return QueueBuilder.durable("stock.restore.queue").build();
    }

    @Bean
    public Binding stockRestoreBinding(Queue stockRestoreQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(stockRestoreQueue).to(productExchange).with(STOCK_RESTORE_KEY);
    }

    @Bean
    public Queue orderCancelledNotifyQueue() {
        return QueueBuilder.durable("order.cancelled.notify.queue").build();
    }

    @Bean
    public Binding orderCancelledNotifyBinding(Queue orderCancelledNotifyQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(orderCancelledNotifyQueue).to(productExchange).with(ORDER_CANCELLED_NOTIFY_KEY);
    }

    @Bean
    public Queue flashStockSyncQueue() {
        return QueueBuilder.durable("flash.stock.sync.queue").build();
    }

    @Bean
    public Binding flashStockSyncBinding(Queue flashStockSyncQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(flashStockSyncQueue).to(productExchange).with(FLASH_STOCK_SYNC_KEY);
    }

    @Bean
    public Queue flashStockRestoreQueue() {
        return QueueBuilder.durable("flash.stock.restore.queue").build();
    }

    @Bean
    public Binding flashStockRestoreBinding(Queue flashStockRestoreQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(flashStockRestoreQueue).to(productExchange).with(FLASH_STOCK_RESTORE_KEY);
    }

    @Bean
    public Queue flashSaleRefundQueue() {
        return QueueBuilder.durable("flash.sale.refund.queue").build();
    }

    @Bean
    public Binding flashSaleRefundBinding(Queue flashSaleRefundQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(flashSaleRefundQueue).to(productExchange).with(FLASH_SALE_REFUND_KEY);
    }

    // ==================== 秒杀订单支付成功通知 ====================

    @Bean
    public Queue flashOrderPaidQueue() {
        return QueueBuilder.durable("flash.order.paid.queue").build();
    }

    @Bean
    public Binding flashOrderPaidBinding(Queue flashOrderPaidQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(flashOrderPaidQueue).to(productExchange).with(FLASH_ORDER_PAID_KEY);
    }

    // ==================== 秒杀订单超时延迟队列 ====================

    @Bean
    public Queue flashTimeoutDelayQueue() {
        return QueueBuilder.durable("flash.timeout.delay.queue").build();
    }

    @Bean
    public Binding flashTimeoutDelayBinding(Queue flashTimeoutDelayQueue, Exchange delayExchange) {
        return new Binding(flashTimeoutDelayQueue.getName(), Binding.DestinationType.QUEUE,
                delayExchange.getName(), FLASH_TIMEOUT_DELAY_KEY, null);
    }

    // ==================== DLQ：普通订单创建失败 Redis 回滚兜底 ====================

    @Bean
    public Queue orderCreateFailedDlq() {
        return QueueBuilder.durable("order.create.failed.dlq").build();
    }

    @Bean
    public Binding orderCreateFailedDlqBinding(Queue orderCreateFailedDlq, DirectExchange errorMessageExchange) {
        return BindingBuilder.bind(orderCreateFailedDlq).to(errorMessageExchange).with(ORDER_CREATE_FAILED_KEY);
    }
}
