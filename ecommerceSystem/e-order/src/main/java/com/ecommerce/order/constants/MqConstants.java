package com.ecommerce.order.constants;

/**
 * e-order 模块 MQ 队列常量。
 * 交换机和 RoutingKey 使用 e-common 中的 {@link com.ecommerce.common.constants.MqConstants}。
 */
public interface MqConstants {

    /**
     * 购物车落库队列 —— 接收延迟消息，消费端比对版本号后执行 DB 同步。
     * 生产者：{@code ECartServiceImpl.scheduleSync()} 通过 {@code RabbitMqHelper.sendDelayMessage()} 发送。
     * 消费者：{@code CartSyncListener.onCartSyncMessage()}。
     * 交换机：{@link com.ecommerce.common.constants.MqConstants.Exchange#DELAY_EXCHANGE}
     * 路由键：{@link com.ecommerce.common.constants.MqConstants.Key#CART_SYNC_KEY}
     */
    String CART_SYNC_QUEUE = "cart.sync.queue";

    /**
     * 优惠券领券落库队列 —— 接收异步消息，消费端用乐观锁持久化领券记录到 DB。
     * 生产者：{@code EUserCouponServiceImpl.claim()} 通过 {@code RabbitMqHelper.sendAsync()} 发送。
     * 消费者：{@code CouponClaimListener.onClaim()}。
     * 交换机：{@link com.ecommerce.common.constants.MqConstants.Exchange#COUPON_EXCHANGE}
     * 路由键：{@link com.ecommerce.common.constants.MqConstants.Key#COUPON_CLAIM_KEY}
     */
    String COUPON_CLAIM_QUEUE = "coupon.claim.queue";

    /** 库存同步落库队列 */
    String STOCK_SYNC_QUEUE = "stock.sync.queue";

    /** 订单超时取消延迟队列 */
    String ORDER_DELAY_QUEUE = "order.delay.queue";

    /** 秒杀订单创建队列 */
    String FLASH_ORDER_CREATE_QUEUE = "flash.order.create.queue";
}
