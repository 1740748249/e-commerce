package com.ecommerce.notification.constants;

/**
 * notification-service MQ 队列常量。
 * 交换机和 RoutingKey 使用 e-common 中的 {@link com.ecommerce.common.constants.MqConstants}。
 */
public interface MqConstants {
    /** 订单通知队列 —— 接收 order-service 发送的通知事件 */
    String ORDER_NOTIFY_QUEUE = "order.notify.queue";
}
